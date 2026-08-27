package com.crudao.kanban.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.Transicao;
import com.crudao.kanban.domain.workflow.TransicaoRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

/** CRUD de Workflow/Etapa/Transicao — TASK-03.2 (RF-002, RF-009, RF-010, RN-003, RN-005). */
@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock private WorkflowRepository workflowRepository;
    @Mock private EtapaRepository etapaRepository;
    @Mock private TransicaoRepository transicaoRepository;
    @Mock private ProjetoRepository projetoRepository;
    @Mock private TarefaRepository tarefaRepository;
    @Mock private PermissaoGuard permissaoGuard;

    private WorkflowService service;

    private final UUID projetoId = UUID.randomUUID();
    private final UUID workflowId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service =
                new WorkflowService(
                        workflowRepository,
                        etapaRepository,
                        transicaoRepository,
                        projetoRepository,
                        tarefaRepository,
                        permissaoGuard);
    }

    @Test
    void criarWorkflow_semPermissao_lanca403SemSalvar() {
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigir(projetoId, "workflow:administrar");

        assertThatThrownBy(() -> service.criar(projetoId, new CriarWorkflowRequest("Padrão")))
                .isInstanceOf(AccessDeniedException.class);
        verify(workflowRepository, never()).save(any());
    }

    @Test
    void criarWorkflow_projetoFinalizado_lanca403() {
        doThrow(new AccessDeniedException("Acesso negado")).when(permissaoGuard).exigirProjetoAtivo(projetoId);

        assertThatThrownBy(() -> service.criar(projetoId, new CriarWorkflowRequest("Padrão")))
                .isInstanceOf(AccessDeniedException.class);
        verify(workflowRepository, never()).save(any());
    }

    @Test
    void criarWorkflow_nomeVazio_lanca422() {
        assertThatThrownBy(() -> service.criar(projetoId, new CriarWorkflowRequest(" ")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(workflowRepository, never()).save(any());
    }

    @Test
    void criarWorkflow_autorizado_persisteVinculadoAoProjeto() {
        when(workflowRepository.findByProjetoId(projetoId)).thenReturn(List.of());
        when(projetoRepository.getReferenceById(projetoId)).thenReturn(projetoRef());
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowResponse resposta = service.criar(projetoId, new CriarWorkflowRequest("Padrão"));

        assertThat(resposta.nome()).isEqualTo("Padrão");
    }

    @Test
    void criarWorkflow_projetoJaTemWorkflow_lanca409SemSalvar() {
        when(workflowRepository.findByProjetoId(projetoId)).thenReturn(List.of(workflow()));

        assertThatThrownBy(() -> service.criar(projetoId, new CriarWorkflowRequest("Outro")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(workflowRepository, never()).save(any());
    }

    @Test
    void criarEtapa_autorizado_persisteComOrdemInformada() {
        Workflow workflow = workflow();
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));
        when(etapaRepository.save(any(Etapa.class))).thenAnswer(inv -> inv.getArgument(0));

        EtapaResponse resposta = service.criarEtapa(workflowId, new CriarEtapaRequest("A Fazer", 0, false));

        assertThat(resposta.nome()).isEqualTo("A Fazer");
        assertThat(resposta.ordem()).isEqualTo(0);
        assertThat(resposta.etapaFinal()).isFalse();
    }

    @Test
    void atualizarTransicoes_etapaNaoFinalSemDestinos_lanca422() {
        Etapa etapa = etapa(UUID.randomUUID(), 0, false);
        when(etapaRepository.findById(etapa.getId())).thenReturn(Optional.of(etapa));

        assertThatThrownBy(() -> service.atualizarTransicoes(etapa.getId(), new AtualizarTransicoesRequest(List.of())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(transicaoRepository, never()).save(any());
    }

    @Test
    void atualizarTransicoes_etapaFinalSemDestinos_permiteVazio() {
        Etapa etapa = etapa(UUID.randomUUID(), 0, true);
        when(etapaRepository.findById(etapa.getId())).thenReturn(Optional.of(etapa));

        TransicoesResponse resposta =
                service.atualizarTransicoes(etapa.getId(), new AtualizarTransicoesRequest(List.of()));

        assertThat(resposta.transicoesSaida()).isEmpty();
    }

    @Test
    void atualizarTransicoes_etapaNaoFinalComDestino_substituiConjunto() {
        Etapa origem = etapa(UUID.randomUUID(), 0, false);
        Etapa destino = etapa(UUID.randomUUID(), 1, true);
        when(etapaRepository.findById(origem.getId())).thenReturn(Optional.of(origem));
        when(etapaRepository.findById(destino.getId())).thenReturn(Optional.of(destino));
        when(transicaoRepository.save(any(Transicao.class))).thenAnswer(inv -> inv.getArgument(0));

        TransicoesResponse resposta =
                service.atualizarTransicoes(
                        origem.getId(), new AtualizarTransicoesRequest(List.of(destino.getId())));

        assertThat(resposta.transicoesSaida()).containsExactly(destino.getId());
        verify(transicaoRepository).deleteByEtapaOrigemId(origem.getId());
    }

    @Test
    void atualizarTransicoes_destinoDeOutroWorkflow_lanca422SemSalvar() {
        Workflow workflowA = workflow();
        Workflow workflowB = new Workflow();
        workflowB.setId(UUID.randomUUID());
        workflowB.setProjeto(projetoRef());
        workflowB.setNome("Outro");
        Etapa origem = etapaNoWorkflow(workflowA, 0, false);
        Etapa destinoDeOutroWorkflow = etapaNoWorkflow(workflowB, 0, true);
        when(etapaRepository.findById(origem.getId())).thenReturn(Optional.of(origem));
        when(etapaRepository.findById(destinoDeOutroWorkflow.getId())).thenReturn(Optional.of(destinoDeOutroWorkflow));

        assertThatThrownBy(
                        () ->
                                service.atualizarTransicoes(
                                        origem.getId(),
                                        new AtualizarTransicoesRequest(List.of(destinoDeOutroWorkflow.getId()))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(transicaoRepository, never()).save(any());
        verify(transicaoRepository, never()).deleteByEtapaOrigemId(any());
    }

    @Test
    void atualizarTransicoes_destinoIgualOrigem_lanca422SemSalvar() {
        Etapa etapa = etapa(UUID.randomUUID(), 0, false);
        when(etapaRepository.findById(etapa.getId())).thenReturn(Optional.of(etapa));

        assertThatThrownBy(
                        () ->
                                service.atualizarTransicoes(
                                        etapa.getId(), new AtualizarTransicoesRequest(List.of(etapa.getId()))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(transicaoRepository, never()).save(any());
    }

    @Test
    void atualizarTransicoes_transicaoDuplicada_lanca409() {
        Etapa origem = etapa(UUID.randomUUID(), 0, false);
        Etapa destino = etapa(UUID.randomUUID(), 1, true);
        origem.setWorkflow(destino.getWorkflow());
        when(etapaRepository.findById(origem.getId())).thenReturn(Optional.of(origem));
        when(etapaRepository.findById(destino.getId())).thenReturn(Optional.of(destino));
        when(transicaoRepository.save(any(Transicao.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(
                        () ->
                                service.atualizarTransicoes(
                                        origem.getId(), new AtualizarTransicoesRequest(List.of(destino.getId()))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    void criarEtapa_ordemDuplicada_lanca409() {
        Workflow workflow = workflow();
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));
        when(etapaRepository.save(any(Etapa.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.criarEtapa(workflowId, new CriarEtapaRequest("A Fazer", 0, false)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    void criarEtapa_ordemNegativa_lanca422() {
        Workflow workflow = workflow();
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));

        assertThatThrownBy(() -> service.criarEtapa(workflowId, new CriarEtapaRequest("A Fazer", -1, false)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(etapaRepository, never()).save(any());
    }

    @Test
    void editarEtapa_tornandoNaoFinalSemTransicaoDeSaida_lanca422() {
        Etapa etapa = etapa(UUID.randomUUID(), 0, true);
        when(etapaRepository.findById(etapa.getId())).thenReturn(Optional.of(etapa));
        when(transicaoRepository.findByEtapaOrigemId(etapa.getId())).thenReturn(List.of());

        assertThatThrownBy(
                        () -> service.editarEtapa(etapa.getId(), new EditarEtapaRequest("Fazendo", 0, false)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(etapaRepository, never()).save(any());
    }

    @Test
    void editarEtapa_reordenando_recalculaOrdemDasDemais() {
        Workflow workflow = workflow();
        Etapa e0 = etapaNoWorkflow(workflow, 0, true);
        Etapa e1 = etapaNoWorkflow(workflow, 1, true);
        Etapa e2 = etapaNoWorkflow(workflow, 2, true);
        when(etapaRepository.findById(e2.getId())).thenReturn(Optional.of(e2));
        when(etapaRepository.findByWorkflowIdOrderByOrdem(workflowId)).thenReturn(List.of(e0, e1, e2));
        when(etapaRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        service.editarEtapa(e2.getId(), new EditarEtapaRequest("Feito", 0, true));

        assertThat(e2.getOrdem()).isEqualTo(0);
        assertThat(e0.getOrdem()).isEqualTo(1);
        assertThat(e1.getOrdem()).isEqualTo(2);
    }

    @Test
    void excluirWorkflow_semTarefasAtivas_permiteExclusao() {
        Workflow workflow = workflow();
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));
        when(etapaRepository.findByWorkflowIdOrderByOrdem(workflowId)).thenReturn(List.of());
        when(tarefaRepository.existsByWorkflowIdAndEtapaAtualEtapaFinalFalse(workflowId)).thenReturn(false);

        service.excluirWorkflow(workflowId);

        verify(workflowRepository).delete(workflow);
    }

    @Test
    void excluirWorkflow_comTarefaAtiva_lanca409SemExcluir() {
        Workflow workflow = workflow();
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));
        when(tarefaRepository.existsByWorkflowIdAndEtapaAtualEtapaFinalFalse(workflowId)).thenReturn(true);

        assertThatThrownBy(() -> service.excluirWorkflow(workflowId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(workflowRepository, never()).delete(any());
    }

    @Test
    void excluirEtapa_comTarefaAtiva_lanca409SemExcluir() {
        Etapa etapa = new Etapa();
        etapa.setId(UUID.randomUUID());
        etapa.setWorkflow(workflow());
        when(etapaRepository.findById(etapa.getId())).thenReturn(Optional.of(etapa));
        when(tarefaRepository.existsByEtapaAtualIdAndEtapaAtualEtapaFinalFalse(etapa.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.excluirEtapa(etapa.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(etapaRepository, never()).delete(any());
    }

    private Projeto projetoRef() {
        Projeto projeto = new Projeto();
        projeto.setId(projetoId);
        return projeto;
    }

    private Workflow workflow() {
        Workflow workflow = new Workflow();
        workflow.setId(workflowId);
        workflow.setProjeto(projetoRef());
        workflow.setNome("Padrão");
        return workflow;
    }

    private Etapa etapa(UUID id, int ordem, boolean etapaFinal) {
        return etapaNoWorkflow(workflow(), ordem, etapaFinal, id);
    }

    private Etapa etapaNoWorkflow(Workflow workflow, int ordem, boolean etapaFinal) {
        return etapaNoWorkflow(workflow, ordem, etapaFinal, UUID.randomUUID());
    }

    private Etapa etapaNoWorkflow(Workflow workflow, int ordem, boolean etapaFinal, UUID id) {
        Etapa etapa = new Etapa();
        etapa.setId(id);
        etapa.setWorkflow(workflow);
        etapa.setNome("Etapa");
        etapa.setOrdem(ordem);
        etapa.setEtapaFinal(etapaFinal);
        return etapa;
    }
}
