package com.crudao.kanban.multipod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.crudao.kanban.KanbanApplication;
import com.crudao.kanban.domain.papel.Papel;
import com.crudao.kanban.domain.papel.PapelRepository;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapel;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.evento.EventoBoardPublisher;
import com.crudao.kanban.evento.EventoBoardPublisher.EventoBoardPayload;
import com.crudao.kanban.evento.NotificacaoEventPublisher;
import com.crudao.kanban.evento.NotificacaoEventPublisher.NotificacaoEventPayload;
import com.crudao.kanban.evento.adapter.ListenNotifyPublisher;
import com.crudao.kanban.websocket.WsTicketService;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * TASK-08.1 — Testes multi-pod e WebSocket (RNF-001 / RNF-002).
 *
 * <p>Sobe <b>duas instâncias Spring Boot completas</b> (o {@code @SpringBootTest} é o pod A; o pod B é
 * um segundo {@link ConfigurableApplicationContext} iniciado via {@link SpringApplicationBuilder})
 * compartilhando o <b>mesmo PostgreSQL</b> (profile {@code it}, banco {@code kanban_it}). Valida o
 * requisito de escalabilidade horizontal do ADR-004: um evento publicado por um pod (NOTIFY no
 * Postgres) é entregue a um cliente STOMP conectado a <b>outro</b> pod, via o {@code LISTEN} daquele
 * pod e o seu SimpleBroker.
 *
 * <p>Casos:
 * <ol>
 *   <li>{@code eventoBoardDoPodBChegaAoClienteDoPodA} — {@link RepeatedTest} 10×: gate de flakiness
 *       do critério de aceite ("0 falhas em 10 execuções consecutivas"). Evento de board publicado
 *       pelo pod B chega ao cliente do pod A em &lt; 2s (RNF-001).</li>
 *   <li>{@code notificacaoDoPodBChegaAoClienteDoPodA} — mesma cadeia para o canal de notificações
 *       (RF-005 sob RNF-002, achado do Comitê).</li>
 *   <li>{@code duasConexoesEmPodsDistintosRecebemOMesmoEvento} — um SUBSCRIBE no pod A e outro no
 *       pod B recebem o mesmo evento publicado uma única vez, ambos em &lt; 2s.</li>
 * </ol>
 *
 * <p>A resincronização client-side por gap de {@code seq} é coberta no frontend
 * ({@code frontend/stomp.test.ts}), pois é lógica do {@code StompManager}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("TASK-08.1: broadcast multi-pod via LISTEN/NOTIFY + WebSocket")
class MultiPodBroadcastIntegrationTest {

  private static final String EMAIL = "multipod-smoke@crudao.local";
  private static final Duration LIMITE_ENTREGA = Duration.ofSeconds(2); // RNF-001

  /** Pod B: segunda instância completa, mesmo Postgres, porta própria. */
  private ConfigurableApplicationContext podB;

  @LocalServerPort private int portaPodA;

  @Autowired private Environment env;
  @Autowired private WsTicketService wsTicketService;
  @Autowired private ListenNotifyPublisher listenNotifyPublisherPodA;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private ProjetoRepository projetoRepository;
  @Autowired private PapelRepository papelRepository;
  @Autowired private UsuarioProjetoPapelRepository uppRepository;

  private Usuario usuario;
  private Projeto projeto;
  private WebSocketStompClient stompClient;

  @BeforeAll
  void subirPodB() {
    // Pod B é uma instância Spring Boot independente apontando para o MESMO Postgres do pod A
    // (datasource resolvido do Environment do pod A e repassado como argumentos de linha de
    // comando — precedência acima dos application-*.yml, senão o application-dev.yml venceria).
    // Flyway desligado: o schema já foi migrado pelo pod A — evita disputa de lock de migração.
    podB =
        new SpringApplicationBuilder(KanbanApplication.class)
            .run(
                "--spring.profiles.active=it",
                "--server.port=0",
                "--spring.jmx.enabled=false",
                "--spring.flyway.enabled=false",
                "--spring.datasource.url=" + env.getProperty("spring.datasource.url"),
                "--spring.datasource.username=" + env.getProperty("spring.datasource.username"),
                "--spring.datasource.password=" + env.getProperty("spring.datasource.password"),
                "--app.keycloak.issuer-uri=" + env.getProperty("app.keycloak.issuer-uri"),
                "--spring.security.oauth2.resourceserver.opaquetoken.introspection-uri="
                    + env.getProperty(
                        "spring.security.oauth2.resourceserver.opaquetoken.introspection-uri"),
                "--kanban.ws-ticket.secret=" + env.getProperty("kanban.ws-ticket.secret"));
  }

  @AfterAll
  void derrubarPodB() {
    if (podB != null) {
      podB.close();
      podB = null;
    }
  }

  @BeforeEach
  void seed() {
    limpar();

    usuario = new Usuario();
    usuario.setKeycloakSub("multipod-" + UUID.randomUUID());
    usuario.setNome("MultiPod Smoke");
    usuario.setEmail(EMAIL);
    usuario.setAtivo(true);
    usuario = usuarioRepository.save(usuario);

    projeto = new Projeto();
    projeto.setNome("MultiPod Smoke Projeto");
    projeto.setDescricao("Projeto do teste multi-pod");
    projeto.setStatus(Projeto.Status.ATIVO);
    projeto.setCriadoPor(usuario);
    projeto.setCriadoEm(OffsetDateTime.now());
    projeto = projetoRepository.save(projeto);

    Papel papel = new Papel();
    papel.setProjeto(projeto);
    papel.setChave("membro");
    papel.setNome("Membro");
    papel = papelRepository.save(papel);

    uppRepository.save(new UsuarioProjetoPapel(usuario, projeto, papel, OffsetDateTime.now()));

    stompClient = new WebSocketStompClient(new StandardWebSocketClient());
    stompClient.setMessageConverter(new MappingJackson2MessageConverter());
  }

  @AfterEach
  void tearDown() {
    if (stompClient != null) {
      stompClient.stop();
    }
    limpar();
  }

  private void limpar() {
    projetoRepository.findAll().stream()
        .filter(p -> "MultiPod Smoke Projeto".equals(p.getNome()))
        .forEach(
            p -> {
              uppRepository.findByProjetoId(p.getId()).forEach(uppRepository::delete);
              papelRepository.findAll().stream()
                  .filter(pa -> pa.getProjeto() != null && p.getId().equals(pa.getProjeto().getId()))
                  .forEach(papelRepository::delete);
              projetoRepository.delete(p);
            });
    usuarioRepository.findByEmail(EMAIL).ifPresent(usuarioRepository::delete);
  }

  // ------------------------------------------------------------------

  private StompSession conectar(int porta, String ticket) throws Exception {
    return stompClient
        .connectAsync(
            "ws://localhost:" + porta + "/ws?ticket=" + ticket,
            new StompSessionHandlerAdapter() {})
        .get(5, TimeUnit.SECONDS);
  }

  private static <T> StompFrameHandler coletorPara(BlockingQueue<T> fila, Class<T> tipo) {
    return new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return tipo;
      }

      @Override
      @SuppressWarnings("unchecked")
      public void handleFrame(StompHeaders headers, Object payload) {
        fila.add((T) payload);
      }
    };
  }

  private int portaPodB() {
    return Integer.parseInt(podB.getEnvironment().getProperty("local.server.port"));
  }

  // ------------------------------------------------------------------

  @RepeatedTest(10)
  @DisplayName("evento de board publicado no pod B chega ao cliente do pod A em < 2s (10×)")
  void eventoBoardDoPodBChegaAoClienteDoPodA() throws Exception {
    EventoBoardPublisher publisherPodB = podB.getBean(EventoBoardPublisher.class);
    await().atMost(Duration.ofSeconds(15)).until(listenNotifyPublisherPodA::isConectado);

    BlockingQueue<Map> recebidas = new LinkedBlockingQueue<>();
    StompSession session = conectar(portaPodA, wsTicketService.emitir(EMAIL));
    session.subscribe("/topic/board/" + projeto.getId(), coletorPara(recebidas, Map.class));
    Thread.sleep(300); // folga para o SUBSCRIBE ser processado pelo interceptor antes do NOTIFY

    publisherPodB.publicar(
        new EventoBoardPayload("TAREFA_MOVIDA", projeto.getId(), 1L, "{\"tarefaId\":\"x\"}"));

    Map frame = recebidas.poll(LIMITE_ENTREGA.toMillis(), TimeUnit.MILLISECONDS);
    assertThat(frame).as("evento do pod B deve chegar ao cliente do pod A em < 2s").isNotNull();
    assertThat(((Map<?, ?>) frame.get("data")).get("tipo")).isEqualTo("TAREFA_MOVIDA");
    assertThat(((Map<?, ?>) frame.get("data")).get("projetoId")).isEqualTo(projeto.getId().toString());
  }

  @Test
  @DisplayName("notificação publicada no pod B chega ao cliente do pod A (RF-005 multi-pod)")
  void notificacaoDoPodBChegaAoClienteDoPodA() throws Exception {
    NotificacaoEventPublisher publisherPodB = podB.getBean(NotificacaoEventPublisher.class);

    BlockingQueue<Map> recebidas = new LinkedBlockingQueue<>();
    StompSession session = conectar(portaPodA, wsTicketService.emitir(EMAIL));
    session.subscribe(
        "/topic/notificacoes/" + usuario.getId(), coletorPara(recebidas, Map.class));
    Thread.sleep(300);

    publisherPodB.publicar(
        new NotificacaoEventPayload(
            "IMPEDIMENTO_MARCADO", usuario.getId(), UUID.randomUUID(), 1L, "{\"tipo\":\"x\"}"));

    Map frame = recebidas.poll(LIMITE_ENTREGA.toMillis(), TimeUnit.MILLISECONDS);
    assertThat(frame).as("notificação do pod B deve chegar ao cliente do pod A").isNotNull();
    assertThat(((Map<?, ?>) frame.get("data")).get("tipo")).isEqualTo("IMPEDIMENTO_MARCADO");
  }

  @Test
  @DisplayName("SUBSCRIBE no pod A e no pod B recebem o mesmo evento publicado uma vez (< 2s)")
  void duasConexoesEmPodsDistintosRecebemOMesmoEvento() throws Exception {
    ListenNotifyPublisher publisherPodB = podB.getBean(ListenNotifyPublisher.class);
    await().atMost(Duration.ofSeconds(15)).until(listenNotifyPublisherPodA::isConectado);
    await().atMost(Duration.ofSeconds(15)).until(publisherPodB::isConectado);

    BlockingQueue<Map> filaPodA = new LinkedBlockingQueue<>();
    BlockingQueue<Map> filaPodB = new LinkedBlockingQueue<>();

    StompSession sessaoPodA = conectar(portaPodA, wsTicketService.emitir(EMAIL));
    StompSession sessaoPodB = conectar(portaPodB(), wsTicketService.emitir(EMAIL));
    sessaoPodA.subscribe("/topic/board/" + projeto.getId(), coletorPara(filaPodA, Map.class));
    sessaoPodB.subscribe("/topic/board/" + projeto.getId(), coletorPara(filaPodB, Map.class));
    Thread.sleep(400);

    listenNotifyPublisherPodA.publicar(
        new EventoBoardPayload("TAREFA_CRIADA", projeto.getId(), 1L, "{}"));

    assertThat(filaPodA.poll(LIMITE_ENTREGA.toMillis(), TimeUnit.MILLISECONDS))
        .as("cliente do pod A recebe o evento")
        .isNotNull();
    assertThat(filaPodB.poll(LIMITE_ENTREGA.toMillis(), TimeUnit.MILLISECONDS))
        .as("cliente do pod B recebe o mesmo evento (propagação multi-pod)")
        .isNotNull();
  }
}
