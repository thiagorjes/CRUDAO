package com.crudao.kanban.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.crudao.kanban.domain.projeto.Projeto;
import com.crudao.kanban.domain.projeto.ProjetoRepository;
import com.crudao.kanban.domain.tarefa.TarefaMoverRequest;
import com.crudao.kanban.domain.tarefa.TarefaRequest;
import com.crudao.kanban.domain.tarefa.TarefaService;
import com.crudao.kanban.domain.tarefa.TipoTarefa;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.TipoTransicao;
import com.crudao.kanban.domain.workflow.Transicao;
import com.crudao.kanban.domain.workflow.TransicaoRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testa a entrega em tempo real fim a fim (RF-005, RNF-001, ADR-004): publica um evento via
 * pg_notify (disparado pelo {@link TarefaService}) e valida que múltiplos clientes STOMP
 * conectados (simulando observadores em pods diferentes) recebem a atualização em até 2s.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealtimeBoardIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("crudao_test");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @LocalServerPort private int port;

  @Autowired private ProjetoRepository projetoRepository;
  @Autowired private WorkflowRepository workflowRepository;
  @Autowired private EtapaRepository etapaRepository;
  @Autowired private TransicaoRepository transicaoRepository;
  @Autowired private TarefaService tarefaService;

  private UUID projetoId;
  private Etapa backlog;
  private Etapa emAndamento;
  private StompSession sessaoCliente1;
  private StompSession sessaoCliente2;

  @BeforeEach
  void setUp() throws Exception {
    Projeto projeto = new Projeto();
    projeto.setNome("Projeto Realtime");
    projetoId = projetoRepository.save(projeto).getId();

    Workflow workflow = new Workflow();
    workflow.setProjetoId(projetoId);
    workflow.setNome("Fluxo Padrão");
    UUID workflowId = workflowRepository.save(workflow).getId();
    projeto.setWorkflowAtivoId(workflowId);
    projetoRepository.save(projeto);

    backlog = criarEtapa(workflowId, "Backlog", 1, false);
    emAndamento = criarEtapa(workflowId, "Em Andamento", 2, false);
    criarTransicao(backlog, emAndamento, TipoTransicao.NORMAL);

    sessaoCliente1 = conectar();
    sessaoCliente2 = conectar();
  }

  @AfterEach
  void tearDown() {
    if (sessaoCliente1 != null) {
      sessaoCliente1.disconnect();
    }
    if (sessaoCliente2 != null) {
      sessaoCliente2.disconnect();
    }
  }

  @Test
  void eventoDeMovimentacaoChegaATodosOsClientesConectadosEmAte2Segundos() throws Exception {
    BlockingQueue<EventoBoardDTO> filaCliente1 = new LinkedBlockingQueue<>();
    BlockingQueue<EventoBoardDTO> filaCliente2 = new LinkedBlockingQueue<>();
    inscrever(sessaoCliente1, filaCliente1);
    inscrever(sessaoCliente2, filaCliente2);

    var tarefa =
        tarefaService.criar(
            new TarefaRequest(
                projetoId, backlog.getId(), null, TipoTarefa.FEATURE, "Tarefa X", null, null));
    // Drena o evento de criação para isolar o evento de movimentação nesta asserção.
    filaCliente1.poll(2, TimeUnit.SECONDS);
    filaCliente2.poll(2, TimeUnit.SECONDS);

    tarefaService.mover(tarefa.id(), new TarefaMoverRequest(emAndamento.getId()));

    EventoBoardDTO eventoCliente1 = filaCliente1.poll(2, TimeUnit.SECONDS);
    EventoBoardDTO eventoCliente2 = filaCliente2.poll(2, TimeUnit.SECONDS);

    assertThat(eventoCliente1).isNotNull();
    assertThat(eventoCliente2).isNotNull();
    assertThat(eventoCliente1.tipo()).isEqualTo(TipoEventoBoard.TAREFA_MOVIDA);
    assertThat(eventoCliente1.etapaAtualId()).isEqualTo(emAndamento.getId());
    assertThat(eventoCliente2.etapaAtualId()).isEqualTo(emAndamento.getId());
  }

  @Test
  void observadoresDaTarefaSaoIncluidosNoEventoEntregueEmTodaTransicaoDeEtapa() throws Exception {
    BlockingQueue<EventoBoardDTO> fila = new LinkedBlockingQueue<>();
    inscrever(sessaoCliente1, fila);

    var tarefa =
        tarefaService.criar(
            new TarefaRequest(
                projetoId, backlog.getId(), null, TipoTarefa.FEATURE, "Tarefa Observada", null,
                null));
    fila.poll(2, TimeUnit.SECONDS); // drena evento de criação

    UUID observadorId = UUID.randomUUID();
    tarefaService.adicionarObservador(tarefa.id(), observadorId);

    tarefaService.mover(tarefa.id(), new TarefaMoverRequest(emAndamento.getId()));

    EventoBoardDTO evento = fila.poll(2, TimeUnit.SECONDS);

    assertThat(evento).isNotNull();
    assertThat(evento.observadorIds()).containsExactly(observadorId);
  }

  private StompSession conectar() throws Exception {
    WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
    client.setMessageConverter(new MappingJackson2MessageConverter());
    return client
        .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
        .get(5, TimeUnit.SECONDS);
  }

  private void inscrever(StompSession sessao, BlockingQueue<EventoBoardDTO> fila) {
    sessao.subscribe(
        "/topic/projetos/" + projetoId + "/board",
        new StompFrameHandler() {
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return EventoBoardDTO.class;
          }

          @Override
          public void handleFrame(StompHeaders headers, Object payload) {
            fila.add((EventoBoardDTO) payload);
          }
        });
  }

  private Etapa criarEtapa(UUID workflowId, String nome, int ordem, boolean etapaFinal) {
    Etapa etapa = new Etapa();
    etapa.setWorkflow(workflowRepository.getReferenceById(workflowId));
    etapa.setNome(nome);
    etapa.setOrdem(ordem);
    etapa.setEtapaFinal(etapaFinal);
    return etapaRepository.save(etapa);
  }

  private void criarTransicao(Etapa origem, Etapa destino, TipoTransicao tipo) {
    Transicao transicao = new Transicao();
    transicao.setEtapaOrigem(origem);
    transicao.setEtapaDestino(destino);
    transicao.setTipo(tipo);
    transicaoRepository.save(transicao);
  }
}
