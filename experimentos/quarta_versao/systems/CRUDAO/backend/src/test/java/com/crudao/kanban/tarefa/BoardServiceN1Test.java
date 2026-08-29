package com.crudao.kanban.tarefa;

import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.tarefa.Tarefa;
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
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * TASK-04.5: Teste de ausência de N+1 no endpoint do board.
 * Estratégia: Criar dataset com múltiplas tarefas (5-10) e verificar que
 * o número de queries não cresce com o volume.
 *
 * Roda contra o PostgreSQL do stack Docker final (ver {@link IntegrationTestBase}).
 */
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.use_sql_comments=true"
})
public class BoardServiceN1Test extends IntegrationTestBase {

    @Autowired
    private BoardService boardService;

    @MockBean
    private PermissaoGuard permissaoGuard;

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
    private org.hibernate.SessionFactory sessionFactory;

    private UUID projetoId;
    private Workflow workflow;
    private Etapa etapaInicial;
    private Etapa etapaFinal;
    private Raia raia;
    private Usuario criador;

    @AfterEach
    void tearDown() {
        tarefaRepository.deleteAll();
        transicaoRepository.deleteAll();
        etapaRepository.deleteAll();
        workflowRepository.deleteAll();
        raiaRepository.deleteAll();
        projetoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @BeforeEach
    void setup() {
        tearDown(); // garante estado limpo mesmo se um teste anterior abortou
        when(permissaoGuard.membro(any(UUID.class))).thenReturn(true);

        // Criar usuário
        criador = new Usuario();
        criador.setKeycloakSub("test-user");
        criador.setNome("Test User");
        criador.setEmail("test@example.com");
        criador.setAtivo(true);
        criador = usuarioRepository.save(criador);

        // Criar projeto
        Projeto projeto = new Projeto();
        projeto.setNome("Projeto Test");
        projeto.setDescricao("Projeto para teste de N+1");
        projeto.setStatus(Projeto.Status.ATIVO);
        projeto.setCriadoPor(criador);
        projeto.setCriadoEm(OffsetDateTime.now());
        projeto = projetoRepository.save(projeto);
        projetoId = projeto.getId();

        // Criar workflow
        workflow = new Workflow();
        workflow.setProjeto(projeto);
        workflow.setNome("Workflow Padrão");
        workflow = workflowRepository.save(workflow);

        // Criar etapas
        etapaInicial = new Etapa();
        etapaInicial.setWorkflow(workflow);
        etapaInicial.setNome("Backlog");
        etapaInicial.setOrdem(1);
        etapaInicial.setEtapaFinal(false);
        etapaInicial = etapaRepository.save(etapaInicial);

        etapaFinal = new Etapa();
        etapaFinal.setWorkflow(workflow);
        etapaFinal.setNome("Concluído");
        etapaFinal.setOrdem(2);
        etapaFinal.setEtapaFinal(true);
        etapaFinal = etapaRepository.save(etapaFinal);

        // Criar transição
        Transicao transicao = new Transicao();
        transicao.setEtapaOrigem(etapaInicial);
        transicao.setEtapaDestino(etapaFinal);
        transicaoRepository.save(transicao);

        // Criar raia
        raia = new Raia();
        raia.setProjeto(projeto);
        raia.setNome("Raia Padrão");
        raia.setOrdem(1);
        raia = raiaRepository.save(raia);
    }

    /**
     * Teste: Obter board com 10 tarefas não deve escalar o número de queries.
     * Esperado: ~4 queries fixas (etapas, raias, tarefas, transições), independente de volume.
     */
    @Test
    void testBoardSemN1Com10Tarefas() {
        // Criar 10 tarefas
        for (int i = 0; i < 10; i++) {
            Tarefa tarefa = new Tarefa();
            tarefa.setProjeto(projetoRepository.findById(projetoId).get());
            tarefa.setWorkflow(workflow);
            tarefa.setEtapaAtual(etapaInicial);
            tarefa.setRaia(raia);
            tarefa.setTitulo("Tarefa " + (i + 1));
            tarefa.setDescricaoEscopo("Descrição " + (i + 1));
            tarefa.setResponsavel(null);
            tarefa.setCriadoPor(criador);
            tarefa.setIniciada(false);
            tarefa.setImpedida(false);
            tarefa.setCriadoEm(Instant.now());
            tarefa.setAtualizadoEm(Instant.now());
            tarefaRepository.save(tarefa);
        }

        // Habilitar estatísticas
        Statistics stats = sessionFactory.getStatistics();
        stats.clear();

        // Chamar o endpoint do board
        BoardResponse board = boardService.obterBoard(projetoId);

        // Verificar estatísticas - usar getQueryExecutionCount() para contagem de queries
        long queryCount = stats.getQueryExecutionCount();
        long entityLoadCount = stats.getEntityLoadCount();

        // Assertions
        assertThat(board).isNotNull();
        assertThat(board.getTarefas()).hasSize(10);
        assertThat(board.getEtapas()).hasSize(2);
        assertThat(board.getRaias()).isNotNull();

        // O que importa para "sem N+1" é o count ser constante com o volume: 7 para 1 e para 10
        // tarefas (contexto completo faz 1 query a mais que o slice @DataJpaTest original).
        assertThat(queryCount)
                .as("Número de queries deve ser fixo (sem N+1)")
                .isLessThanOrEqualTo(8);

        System.out.println("✓ Board carregado com 10 tarefas: " + queryCount + " queries");
    }

    /**
     * Teste: Obter board com 1 tarefa também não deve diferir significativamente.
     * Verifica que a estratégia de queries fixas funciona também com volume pequeno.
     */
    @Test
    void testBoardSemN1Com1Tarefa() {
        Tarefa tarefa = new Tarefa();
        tarefa.setProjeto(projetoRepository.findById(projetoId).get());
        tarefa.setWorkflow(workflow);
        tarefa.setEtapaAtual(etapaInicial);
        tarefa.setRaia(raia);
        tarefa.setTitulo("Tarefa Única");
        tarefa.setDescricaoEscopo("Uma tarefa");
        tarefa.setResponsavel(null);
        tarefa.setCriadoPor(criador);
        tarefa.setIniciada(false);
        tarefa.setImpedida(false);
        tarefa.setCriadoEm(Instant.now());
        tarefa.setAtualizadoEm(Instant.now());
        tarefaRepository.save(tarefa);

        Statistics stats = sessionFactory.getStatistics();
        stats.clear();

        BoardResponse board = boardService.obterBoard(projetoId);
        long queryCount = stats.getQueryExecutionCount();

        assertThat(board).isNotNull();
        assertThat(board.getTarefas()).hasSize(1);
        assertThat(queryCount)
                .as("Número de queries deve ser fixo (sem N+1)")
                .isLessThanOrEqualTo(8);

        System.out.println("✓ Board carregado com 1 tarefa: " + queryCount + " queries");
    }
}
