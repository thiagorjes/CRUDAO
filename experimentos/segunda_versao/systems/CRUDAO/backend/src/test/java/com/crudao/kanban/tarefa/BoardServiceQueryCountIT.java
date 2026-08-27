package com.crudao.kanban.tarefa;

import static org.assertj.core.api.Assertions.assertThat;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.OffsetDateTime;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Valida o critério de aceite explícito da TASK-04.5/{@code data-model.md} "Nota de performance":
 * {@code GET /api/projetos/{id}/board} não pode sofrer N+1 — a contagem de queries deve ser fixa
 * independentemente do volume de tarefas retornadas.
 */
@Testcontainers
@SpringBootTest
class BoardServiceQueryCountIT {

    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @Autowired private BoardService boardService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ProjetoRepository projetoRepository;
    @Autowired private WorkflowRepository workflowRepository;
    @Autowired private EtapaRepository etapaRepository;
    @Autowired private RaiaRepository raiaRepository;
    @Autowired private TarefaRepository tarefaRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @AfterEach
    void limpar() {
        UsuarioAutenticadoHolder.clear();
    }

    @Test
    void contagemDeQueriesDoBoardNaoEscalaComVolumeDeTarefas() {
        Usuario usuario = criarUsuarioAdminGlobal();
        UsuarioAutenticadoHolder.set(usuario);
        Projeto projeto = criarProjeto(usuario);
        Workflow workflow = criarWorkflow(projeto);
        Etapa etapa = criarEtapa(workflow);
        var raiaGlobal = raiaRepository.findByProjetoIdIsNull().get(0);

        criarTarefas(projeto, workflow, etapa, raiaGlobal.getId(), usuario, 3);

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        boardService.board(projeto.getId());
        long queriesCom3Tarefas = statistics.getPrepareStatementCount();

        criarTarefas(projeto, workflow, etapa, raiaGlobal.getId(), usuario, 5);

        statistics.clear();
        boardService.board(projeto.getId());
        long queriesCom8Tarefas = statistics.getPrepareStatementCount();

        assertThat(queriesCom8Tarefas).isEqualTo(queriesCom3Tarefas);
    }

    private void criarTarefas(
            Projeto projeto, Workflow workflow, Etapa etapa, java.util.UUID raiaId, Usuario usuario, int quantidade) {
        var raia = raiaRepository.findById(raiaId).orElseThrow();
        for (int i = 0; i < quantidade; i++) {
            OffsetDateTime agora = OffsetDateTime.now();
            Tarefa tarefa = new Tarefa();
            tarefa.setProjeto(projeto);
            tarefa.setWorkflow(workflow);
            tarefa.setEtapaAtual(etapa);
            tarefa.setRaia(raia);
            tarefa.setTitulo("Tarefa " + i);
            tarefa.setResponsavel(usuario);
            tarefa.setCriadoPor(usuario);
            tarefa.setIniciada(false);
            tarefa.setImpedida(false);
            tarefa.setCriadoEm(agora);
            tarefa.setAtualizadoEm(agora);
            tarefaRepository.save(tarefa);
        }
    }

    private Usuario criarUsuarioAdminGlobal() {
        Usuario usuario = new Usuario();
        usuario.setKeycloakSub("sub-" + java.util.UUID.randomUUID());
        usuario.setNome("Admin Global");
        usuario.setEmail("admin-" + java.util.UUID.randomUUID() + "@teste.com");
        usuario.setAtivo(true);
        usuario.setAdminGlobal(true);
        usuario.setCriadoEm(OffsetDateTime.now());
        return usuarioRepository.save(usuario);
    }

    private Projeto criarProjeto(Usuario criadoPor) {
        Projeto projeto = new Projeto();
        projeto.setNome("Projeto Board Query Count");
        projeto.setStatus(Projeto.Status.ATIVO);
        projeto.setCriadoPor(criadoPor);
        projeto.setCriadoEm(OffsetDateTime.now());
        return projetoRepository.save(projeto);
    }

    private Workflow criarWorkflow(Projeto projeto) {
        Workflow workflow = new Workflow();
        workflow.setProjeto(projeto);
        workflow.setNome("Workflow padrão");
        return workflowRepository.save(workflow);
    }

    private Etapa criarEtapa(Workflow workflow) {
        Etapa etapa = new Etapa();
        etapa.setWorkflow(workflow);
        etapa.setNome("A fazer");
        etapa.setOrdem(0);
        etapa.setEtapaFinal(false);
        return etapaRepository.save(etapa);
    }
}
