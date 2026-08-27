package com.crudao.kanban.domain.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.crudao.kanban.domain.projeto.Projeto;
import com.crudao.kanban.domain.projeto.ProjetoRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testa o fluxo real de domínio (TASK-01.1) contra um PostgreSQL real: criação de projeto,
 * workflow, etapas e transições, e a validação de consistência RN-003.
 */
@Testcontainers
@SpringBootTest
class WorkflowFluxoIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("crudao_test");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private ProjetoRepository projetoRepository;
  @Autowired private WorkflowRepository workflowRepository;
  @Autowired private EtapaRepository etapaRepository;
  @Autowired private TransicaoRepository transicaoRepository;
  @Autowired private WorkflowService workflowService;
  @Autowired private TransicaoEngine transicaoEngine;

  private UUID projetoId;
  private UUID workflowId;

  @BeforeEach
  void setUp() {
    Projeto projeto = new Projeto();
    projeto.setNome("Projeto Alpha");
    projetoId = projetoRepository.save(projeto).getId();

    Workflow workflow = new Workflow();
    workflow.setProjetoId(projetoId);
    workflow.setNome("Fluxo Padrão");
    workflowId = workflowRepository.save(workflow).getId();
  }

  @Test
  void workflowComTodasAsEtapasNaoFinaisComTransicaoDeSaidaEstaConsistente() {
    Etapa backlog = criarEtapa("Backlog", 1, false);
    Etapa emAndamento = criarEtapa("Em Andamento", 2, false);
    Etapa concluido = criarEtapa("Concluído", 3, true);

    criarTransicao(backlog, emAndamento, TipoTransicao.NORMAL);
    criarTransicao(emAndamento, concluido, TipoTransicao.NORMAL);

    List<String> violacoes = workflowService.validarConsistencia(workflowId);

    assertThat(violacoes).isEmpty();
  }

  @Test
  void workflowComEtapaNaoFinalSemTransicaoDeSaidaEstaInconsistente() {
    Etapa backlog = criarEtapa("Backlog", 1, false);
    criarEtapa("Em Andamento", 2, false); // sem transição de saída

    List<String> violacoes = workflowService.validarConsistencia(workflowId);

    assertThat(violacoes).containsExactly("Em Andamento");
  }

  @Test
  void engineDeTransicaoRespeitaAsTransicoesPersistidasNoBanco() {
    Etapa backlog = criarEtapa("Backlog", 1, false);
    Etapa emAndamento = criarEtapa("Em Andamento", 2, false);
    Etapa revisao = criarEtapa("Em Revisão", 3, false);
    criarTransicao(backlog, emAndamento, TipoTransicao.NORMAL);

    List<Transicao> transicoesDoWorkflow = transicaoRepository.findByWorkflowId(workflowId);

    assertThat(transicaoEngine.transicaoPermitida(backlog, emAndamento, transicoesDoWorkflow))
        .isTrue();
    assertThat(transicaoEngine.transicaoPermitida(backlog, revisao, transicoesDoWorkflow))
        .isFalse();
  }

  private Etapa criarEtapa(String nome, int ordem, boolean etapaFinal) {
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
