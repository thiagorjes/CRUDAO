package com.crudao.kanban.workflow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.workflow.*;
import com.crudao.kanban.rbac.PermissaoGuard;
import com.crudao.kanban.workflow.dto.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private EtapaRepository etapaRepository;

    @Mock
    private TransicaoRepository transicaoRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private TarefaRepository tarefaRepository;

    @Mock
    private PermissaoGuard permissaoGuard;

    @InjectMocks
    private WorkflowService workflowService;

    private UUID projetoId;
    private Projeto projeto;

    @BeforeEach
    void setUp() {
        projetoId = UUID.randomUUID();
        projeto = new Projeto();
        projeto.setId(projetoId);
        projeto.setNome("Projeto Teste");
        projeto.setStatus(Projeto.Status.ATIVO);
    }

    @Test
    @DisplayName("test_criarWorkflow_when_projetoExisteEValido_should_salvarERetornar")
    void test_criarWorkflow_when_projetoExisteEValido_should_salvarERetornar() {
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        Workflow savedWorkflow = new Workflow(UUID.randomUUID(), projeto, "Workflow 1");
        when(workflowRepository.save(any(Workflow.class))).thenReturn(savedWorkflow);

        CriarWorkflowRequest req = new CriarWorkflowRequest("Workflow 1");
        WorkflowResponse resp = workflowService.criarWorkflow(projetoId, req);

        assertNotNull(resp.getId());
        assertEquals("Workflow 1", resp.getNome());
        verify(permissaoGuard).exigirProjetoAtivo(projetoId);
        verify(permissaoGuard).exigir(projetoId, "workflow:administrar");
    }

    @Test
    @DisplayName("test_atualizarTransicoes_when_etapaNaoFinalETransicoesVazias_should_retornarErro422")
    void test_atualizarTransicoes_when_etapaNaoFinalETransicoesVazias_should_retornarErro422() {
        UUID etapaId = UUID.randomUUID();
        Workflow workflow = new Workflow(UUID.randomUUID(), projeto, "WF");
        Etapa etapaOrigem = new Etapa(etapaId, workflow, "Em Progresso", 1, false);

        when(etapaRepository.findById(etapaId)).thenReturn(Optional.of(etapaOrigem));

        AtualizarTransicoesRequest request = new AtualizarTransicoesRequest(Collections.emptyList());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            workflowService.atualizarTransicoes(etapaId, request)
        );

        assertEquals(422, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("test_atualizarTransicoes_when_etapaFinalETransicoesVazias_should_permitirESalvar")
    void test_atualizarTransicoes_when_etapaFinalETransicoesVazias_should_permitirESalvar() {
        UUID etapaId = UUID.randomUUID();
        Workflow workflow = new Workflow(UUID.randomUUID(), projeto, "WF");
        Etapa etapaFinal = new Etapa(etapaId, workflow, "Concluído", 2, true);

        when(etapaRepository.findById(etapaId)).thenReturn(Optional.of(etapaFinal));

        AtualizarTransicoesRequest request = new AtualizarTransicoesRequest(Collections.emptyList());

        TransicoesResponse resp = workflowService.atualizarTransicoes(etapaId, request);

        assertEquals(etapaId, resp.getEtapaId());
        assertTrue(resp.getTransicoesSaida().isEmpty());
        verify(transicaoRepository).deleteByEtapaOrigemId(etapaId);
    }

    @Test
    @DisplayName("test_excluirWorkflow_when_semTarefasAtivas_should_excluirSemErro")
    void test_excluirWorkflow_when_semTarefasAtivas_should_excluirSemErro() {
        UUID workflowId = UUID.randomUUID();
        Workflow workflow = new Workflow(workflowId, projeto, "WF");
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));
        when(tarefaRepository.existsByWorkflowId(workflowId)).thenReturn(false);

        assertDoesNotThrow(() -> workflowService.excluirWorkflow(workflowId));

        verify(workflowRepository).delete(workflow);
    }

    @Test
    @DisplayName("test_excluirWorkflow_when_temTarefasAtivas_should_retornarErro409")
    void test_excluirWorkflow_when_temTarefasAtivas_should_retornarErro409() {
        UUID workflowId = UUID.randomUUID();
        Workflow workflow = new Workflow(workflowId, projeto, "WF");
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));
        when(tarefaRepository.existsByWorkflowId(workflowId)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            workflowService.excluirWorkflow(workflowId)
        );

        assertEquals(409, ex.getStatusCode().value());
        verify(workflowRepository, never()).delete(any());
    }
}
