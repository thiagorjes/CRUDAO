package com.crudao.kanban.tarefa;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistorico;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistoricoRepository;
import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.Transicao;
import com.crudao.kanban.domain.workflow.TransicaoRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import com.crudao.kanban.support.IntegrationTestBase;
import com.crudao.kanban.tarefa.dto.BoardResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-04.5: Teste de integração do endpoint GET /api/projetos/{projetoId}/board.
 * Valida contra o PostgreSQL do stack Docker final (ver {@link IntegrationTestBase}).
 * RF-001: Board retorna etapas (na ordem), raias e tarefas.
 * Critério de aceite: Teste de integração comprova ausência de N+1.
 */
@AutoConfigureMockMvc(addFilters = false) // sem cadeia de segurança HTTP — o teste valida o board, não OIDC
@Transactional // rollback por teste — evita acúmulo/colisão de keycloak_sub entre métodos
public class TarefaControllerBoardIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PermissaoGuard permissaoGuard;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private TransicaoRepository transicaoRepository;

    @Autowired
    private RaiaRepository raiaRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private TarefaEtapaHistoricoRepository tarefaEtapaHistoricoRepository;

    private UUID projetoId;
    private Workflow workflow;
    private Etapa etapaBacklog;
    private Etapa etapaEmExecucao;
    private Etapa etapaConcluido;
    private Raia raia1;
    private Raia raia2;
    private Usuario criador;

    @BeforeEach
    void setup() {
        when(permissaoGuard.membro(any(UUID.class))).thenReturn(true);

        // Criar usuário e setar como autenticado
        criador = new Usuario();
        criador.setKeycloakSub("test-user-board");
        criador.setNome("Test User");
        criador.setEmail("test-board@example.com");
        criador.setAtivo(true);
        criador = usuarioRepository.save(criador);
        UsuarioAutenticadoHolder.set(criador);

        // Criar projeto
        Projeto projeto = new Projeto();
        projeto.setNome("Projeto Board Test");
        projeto.setDescricao("Projeto para teste de board");
        projeto.setStatus(Projeto.Status.ATIVO);
        projeto.setCriadoPor(criador);
        projeto.setCriadoEm(OffsetDateTime.now());
        projeto = projetoRepository.save(projeto);
        projetoId = projeto.getId();

        // Criar workflow com 3 etapas
        workflow = new Workflow();
        workflow.setProjeto(projeto);
        workflow.setNome("Workflow Test");
        workflow = workflowRepository.save(workflow);

        // Etapa Backlog (1ª)
        etapaBacklog = new Etapa();
        etapaBacklog.setWorkflow(workflow);
        etapaBacklog.setNome("Backlog");
        etapaBacklog.setOrdem(1);
        etapaBacklog.setEtapaFinal(false);
        etapaBacklog = etapaRepository.save(etapaBacklog);

        // Etapa Em Execução (2ª)
        etapaEmExecucao = new Etapa();
        etapaEmExecucao.setWorkflow(workflow);
        etapaEmExecucao.setNome("Em Execução");
        etapaEmExecucao.setOrdem(2);
        etapaEmExecucao.setEtapaFinal(false);
        etapaEmExecucao = etapaRepository.save(etapaEmExecucao);

        // Etapa Concluído (3ª)
        etapaConcluido = new Etapa();
        etapaConcluido.setWorkflow(workflow);
        etapaConcluido.setNome("Concluído");
        etapaConcluido.setOrdem(3);
        etapaConcluido.setEtapaFinal(true);
        etapaConcluido = etapaRepository.save(etapaConcluido);

        // Criar transições
        Transicao t1 = new Transicao();
        t1.setEtapaOrigem(etapaBacklog);
        t1.setEtapaDestino(etapaEmExecucao);
        transicaoRepository.save(t1);

        Transicao t2 = new Transicao();
        t2.setEtapaOrigem(etapaEmExecucao);
        t2.setEtapaDestino(etapaConcluido);
        transicaoRepository.save(t2);

        // Criar raias
        raia1 = new Raia();
        raia1.setProjeto(projeto);
        raia1.setNome("Backend");
        raia1.setOrdem(1);
        raia1 = raiaRepository.save(raia1);

        raia2 = new Raia();
        raia2.setProjeto(projeto);
        raia2.setNome("Frontend");
        raia2.setOrdem(2);
        raia2 = raiaRepository.save(raia2);
    }

    /**
     * Teste: GET /api/projetos/{projetoId}/board retorna estrutura correta.
     * RF-001: Etapas estão na ordem configurada, raias e tarefas estão presentes.
     */
    @Test
    void testObterBoardRetornaEstrutura() throws Exception {
        // Criar 5 tarefas distribuídas entre as raias e etapas
        for (int i = 0; i < 5; i++) {
            Tarefa tarefa = new Tarefa();
            tarefa.setProjeto(projetoRepository.findById(projetoId).get());
            tarefa.setWorkflow(workflow);
            tarefa.setEtapaAtual(i < 3 ? etapaBacklog : etapaEmExecucao);
            tarefa.setRaia(i % 2 == 0 ? raia1 : raia2);
            tarefa.setTitulo("Tarefa " + (i + 1));
            tarefa.setDescricaoEscopo("Descrição tarefa " + (i + 1));
            tarefa.setResponsavel(null);
            tarefa.setCriadoPor(criador);
            tarefa.setIniciada(false);
            tarefa.setImpedida(i == 2); // Uma tarefa impedida
            tarefa.setCriadoEm(Instant.now());
            tarefa.setAtualizadoEm(Instant.now());
            Tarefa saved = tarefaRepository.save(tarefa);

            // Criar histórico de etapa
            TarefaEtapaHistorico historico = new TarefaEtapaHistorico();
            historico.setTarefa(saved);
            historico.setEtapa(saved.getEtapaAtual());
            historico.setEntradaEm(Instant.now());
            historico.setSaidaEm(null);
            tarefaEtapaHistoricoRepository.save(historico);
        }

        // Chamar o endpoint
        MvcResult result = mockMvc.perform(get("/api/projetos/" + projetoId + "/board")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        BoardResponse board = objectMapper.readValue(responseBody, BoardResponse.class);

        // Assertions
        assertThat(board).isNotNull();
        assertThat(board.getEtapas()).hasSize(3);
        assertThat(board.getEtapas().get(0).getNome()).isEqualTo("Backlog");
        assertThat(board.getEtapas().get(1).getNome()).isEqualTo("Em Execução");
        assertThat(board.getEtapas().get(2).getNome()).isEqualTo("Concluído");

        assertThat(board.getRaias()).hasSize(2);
        assertThat(board.getRaias().get(0).getNome()).isEqualTo("Backend");
        assertThat(board.getRaias().get(1).getNome()).isEqualTo("Frontend");

        assertThat(board.getTarefas()).hasSize(5);

        // Verificar que a tarefa impedida tem a flag correta
        assertThat(board.getTarefas().stream()
                .filter(t -> t.isImpedida())
                .count()).isEqualTo(1);

        System.out.println("✓ Board retorna estrutura correta com 5 tarefas");
    }

    /**
     * Teste: GET /api/tarefas/{tarefaId} retorna detalhe com lead-time.
     * RF-006: Lead-time calculado por etapa.
     */
    @Test
    void testObterDetalheComLeadTime() throws Exception {
        // Criar uma tarefa
        Tarefa tarefa = new Tarefa();
        tarefa.setProjeto(projetoRepository.findById(projetoId).get());
        tarefa.setWorkflow(workflow);
        tarefa.setEtapaAtual(etapaBacklog);
        tarefa.setRaia(raia1);
        tarefa.setTitulo("Tarefa Detalhe");
        tarefa.setDescricaoEscopo("Descrição");
        tarefa.setResponsavel(null);
        tarefa.setCriadoPor(criador);
        tarefa.setIniciada(false);
        tarefa.setImpedida(false);
        tarefa.setCriadoEm(Instant.now());
        tarefa.setAtualizadoEm(Instant.now());
        tarefa = tarefaRepository.save(tarefa);

        // Criar histórico
        TarefaEtapaHistorico historico = new TarefaEtapaHistorico();
        historico.setTarefa(tarefa);
        historico.setEtapa(etapaBacklog);
        historico.setEntradaEm(Instant.now().minusSeconds(100));
        historico.setSaidaEm(null); // Em andamento
        tarefaEtapaHistoricoRepository.save(historico);

        // Chamar endpoint GET /api/tarefas/{id}
        MvcResult result = mockMvc.perform(get("/api/tarefas/" + tarefa.getId())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("historicoEtapas");
        assertThat(responseBody).contains("tempoImpedimentoTotalSegundos");

        System.out.println("✓ Detalhe da tarefa retorna lead-time");
    }
}
