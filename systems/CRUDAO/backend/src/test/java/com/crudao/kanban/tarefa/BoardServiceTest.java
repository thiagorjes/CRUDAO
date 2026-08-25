package com.crudao.kanban.tarefa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.tarefa.TarefaBoardItemResponse;
import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.Transicao;
import com.crudao.kanban.domain.workflow.TransicaoRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import com.crudao.kanban.raia.RaiaResponse;
import com.crudao.kanban.workflow.EtapaResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Estado completo do board (RF-001, TASK-04.5) — casos de borda não cobertos pelo IT de contagem
 * de queries (achado de code review, agent QA): membro negado, projeto sem workflow, projeto sem
 * raias próprias (fallback global).
 */
@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock private WorkflowRepository workflowRepository;
    @Mock private EtapaRepository etapaRepository;
    @Mock private TransicaoRepository transicaoRepository;
    @Mock private RaiaRepository raiaRepository;
    @Mock private TarefaRepository tarefaRepository;
    @Mock private PermissaoGuard permissaoGuard;

    private BoardService service;

    private final UUID projetoId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service =
                new BoardService(
                        workflowRepository, etapaRepository, transicaoRepository, raiaRepository, tarefaRepository, permissaoGuard);
    }

    @Test
    void naoMembroDoProjeto_lancaAccessDenied() {
        when(permissaoGuard.membro(projetoId)).thenReturn(false);

        assertThatThrownBy(() -> service.board(projetoId)).isInstanceOf(AccessDeniedException.class);

        verify(tarefaRepository, never()).buscarItensDoBoard(projetoId);
    }

    @Test
    void projetoSemWorkflow_retornaEtapasVazias() {
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(workflowRepository.findByProjetoId(projetoId)).thenReturn(List.of());
        when(raiaRepository.findByProjetoIdOrderByOrdem(projetoId)).thenReturn(List.of());
        Raia raiaGlobal = raia(UUID.randomUUID(), "Geral", 0);
        when(raiaRepository.findByProjetoIdIsNull()).thenReturn(List.of(raiaGlobal));
        when(tarefaRepository.buscarItensDoBoard(projetoId)).thenReturn(List.of());

        BoardResponse board = service.board(projetoId);

        assertThat(board.etapas()).isEmpty();
        assertThat(board.raias()).hasSize(1);
        assertThat(board.raias().get(0).global()).isTrue();
    }

    @Test
    void projetoSemRaiasProprias_caiParaRaiaDefaultGlobal() {
        Workflow workflow = workflow();
        Etapa etapa = etapa(workflow, "A fazer", 0, false);
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(workflowRepository.findByProjetoId(projetoId)).thenReturn(List.of(workflow));
        when(etapaRepository.findByWorkflowIdOrderByOrdem(workflow.getId())).thenReturn(List.of(etapa));
        when(transicaoRepository.findByEtapaOrigemIdIn(List.of(etapa.getId()))).thenReturn(List.of());
        when(raiaRepository.findByProjetoIdOrderByOrdem(projetoId)).thenReturn(List.of());
        Raia raiaGlobal = raia(UUID.randomUUID(), "Geral", 0);
        when(raiaRepository.findByProjetoIdIsNull()).thenReturn(List.of(raiaGlobal));
        when(tarefaRepository.buscarItensDoBoard(projetoId)).thenReturn(List.of());

        BoardResponse board = service.board(projetoId);

        assertThat(board.raias()).extracting(RaiaResponse::id).containsExactly(raiaGlobal.getId());
        assertThat(board.raias().get(0).global()).isTrue();
    }

    @Test
    void projetoComWorkflowRaiasETarefas_retornaBoardCompleto() {
        Workflow workflow = workflow();
        Etapa etapaInicial = etapa(workflow, "A fazer", 0, false);
        Etapa etapaFinal = etapa(workflow, "Concluído", 1, true);
        Transicao transicao = new Transicao();
        transicao.setEtapaOrigem(etapaInicial);
        transicao.setEtapaDestino(etapaFinal);
        Raia raiaDoProjeto = raia(UUID.randomUUID(), "Time A", 0);
        TarefaBoardItemResponse item =
                new TarefaBoardItemResponse(
                        UUID.randomUUID(), "Card", etapaInicial.getId(), raiaDoProjeto.getId(), null, false, null, false);

        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(workflowRepository.findByProjetoId(projetoId)).thenReturn(List.of(workflow));
        when(etapaRepository.findByWorkflowIdOrderByOrdem(workflow.getId()))
                .thenReturn(List.of(etapaInicial, etapaFinal));
        when(transicaoRepository.findByEtapaOrigemIdIn(List.of(etapaInicial.getId(), etapaFinal.getId())))
                .thenReturn(List.of(transicao));
        when(raiaRepository.findByProjetoIdOrderByOrdem(projetoId)).thenReturn(List.of(raiaDoProjeto));
        when(tarefaRepository.buscarItensDoBoard(projetoId)).thenReturn(List.of(item));

        BoardResponse board = service.board(projetoId);

        assertThat(board.etapas()).hasSize(2);
        EtapaResponse etapaInicialResponse =
                board.etapas().stream().filter(e -> e.id().equals(etapaInicial.getId())).findFirst().orElseThrow();
        assertThat(etapaInicialResponse.transicoesSaida()).containsExactly(etapaFinal.getId());
        assertThat(board.raias()).extracting(RaiaResponse::id).containsExactly(raiaDoProjeto.getId());
        assertThat(board.raias().get(0).global()).isFalse();
        assertThat(board.tarefas()).containsExactly(item);
        verify(raiaRepository, never()).findByProjetoIdIsNull();
    }

    private Workflow workflow() {
        Workflow workflow = new Workflow();
        workflow.setId(UUID.randomUUID());
        return workflow;
    }

    private Etapa etapa(Workflow workflow, String nome, int ordem, boolean etapaFinal) {
        Etapa etapa = new Etapa();
        etapa.setId(UUID.randomUUID());
        etapa.setWorkflow(workflow);
        etapa.setNome(nome);
        etapa.setOrdem(ordem);
        etapa.setEtapaFinal(etapaFinal);
        return etapa;
    }

    private Raia raia(UUID id, String nome, int ordem) {
        Raia raia = new Raia();
        raia.setId(id);
        raia.setNome(nome);
        raia.setOrdem(ordem);
        return raia;
    }
}
