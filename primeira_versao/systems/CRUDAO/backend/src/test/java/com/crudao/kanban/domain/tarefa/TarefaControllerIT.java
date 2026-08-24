package com.crudao.kanban.domain.tarefa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crudao.kanban.domain.projeto.Projeto;
import com.crudao.kanban.domain.projeto.ProjetoRepository;
import com.crudao.kanban.domain.rbac.Papel;
import com.crudao.kanban.domain.rbac.PapelRepository;
import com.crudao.kanban.domain.rbac.Usuario;
import com.crudao.kanban.domain.rbac.UsuarioRepository;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.security.UsuarioLocalDetails;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testa {@code GET /api/tarefas} de ponta a ponta pelo controller (TASK-01.1) — garante que
 * tarefas soft-deleted não aparecem na listagem consumida pelo board (RF-002), o que um teste de
 * {@link TarefaService} isolado não cobre (a rota é o contrato real consumido pelo frontend).
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class TarefaControllerIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("crudao_test");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ProjetoRepository projetoRepository;
  @Autowired private WorkflowRepository workflowRepository;
  @Autowired private EtapaRepository etapaRepository;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private PapelRepository papelRepository;
  @Autowired private TarefaService tarefaService;

  private UUID projetoId;
  private Etapa backlog;
  private Authentication autenticacaoAdmin;

  @BeforeEach
  void setUp() {
    Projeto projeto = new Projeto();
    projeto.setNome("Projeto Controller IT");
    projetoId = projetoRepository.save(projeto).getId();

    Workflow workflow = new Workflow();
    workflow.setProjetoId(projetoId);
    workflow.setNome("Fluxo Padrão");
    UUID workflowId = workflowRepository.save(workflow).getId();
    projeto.setWorkflowAtivoId(workflowId);
    projetoRepository.save(projeto);

    backlog = criarEtapa(workflowId, "Backlog", 1, false);

    Papel papelAdmin =
        papelRepository
            .findByNomeIgnoreCase("admin")
            .orElseThrow(() -> new IllegalStateException("Papel 'admin' não seedado"));
    Usuario admin = new Usuario();
    admin.setEmail("admin-controller-it@crudao.local");
    admin.setNome("Admin Controller IT");
    admin.setPapel(papelAdmin);
    admin.setAdmin(true);
    admin.setSenhaHash("hash-nao-usado-neste-teste");
    admin = usuarioRepository.save(admin);

    autenticacaoAdmin =
        new UsernamePasswordAuthenticationToken(
            new UsuarioLocalDetails(admin), null, new UsuarioLocalDetails(admin).getAuthorities());
  }

  @Test
  void listarPorProjetoNaoRetornaTarefaSoftDeleted() throws Exception {
    var ativa =
        tarefaService.criar(
            new TarefaRequest(
                projetoId, backlog.getId(), null, TipoTarefa.FEATURE, "Tarefa Ativa", null, null));
    var excluida =
        tarefaService.criar(
            new TarefaRequest(
                projetoId,
                backlog.getId(),
                null,
                TipoTarefa.FEATURE,
                "Tarefa Excluída",
                null,
                null));
    tarefaService.excluir(excluida.id());

    String corpo =
        mockMvc
            .perform(
                get("/api/tarefas")
                    .param("projetoId", projetoId.toString())
                    .with(authentication(autenticacaoAdmin)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(corpo).contains(ativa.id().toString());
    assertThat(corpo).doesNotContain(excluida.id().toString());
  }

  @Test
  void buscarTarefaSoftDeletedRetorna404() throws Exception {
    var tarefa =
        tarefaService.criar(
            new TarefaRequest(
                projetoId, backlog.getId(), null, TipoTarefa.FEATURE, "Tarefa X", null, null));
    tarefaService.excluir(tarefa.id());

    mockMvc
        .perform(get("/api/tarefas/" + tarefa.id()).with(authentication(autenticacaoAdmin)))
        .andExpect(status().isNotFound());
  }

  @Test
  void excluirTarefaJaExcluidaRetorna404() throws Exception {
    var tarefa =
        tarefaService.criar(
            new TarefaRequest(
                projetoId, backlog.getId(), null, TipoTarefa.FEATURE, "Tarefa Y", null, null));
    tarefaService.excluir(tarefa.id());

    mockMvc
        .perform(delete("/api/tarefas/" + tarefa.id()).with(authentication(autenticacaoAdmin)))
        .andExpect(status().isNotFound());
  }

  private Etapa criarEtapa(UUID workflowId, String nome, int ordem, boolean etapaFinal) {
    Etapa etapa = new Etapa();
    etapa.setWorkflow(workflowRepository.getReferenceById(workflowId));
    etapa.setNome(nome);
    etapa.setOrdem(ordem);
    etapa.setEtapaFinal(etapaFinal);
    return etapaRepository.save(etapa);
  }
}
