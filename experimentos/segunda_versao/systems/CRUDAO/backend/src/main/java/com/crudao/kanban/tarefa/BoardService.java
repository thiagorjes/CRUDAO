package com.crudao.kanban.tarefa;

import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.tarefa.TarefaBoardItemResponse;
import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.TransicaoRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.raia.RaiaResponse;
import com.crudao.kanban.rbac.PermissaoGuard;
import com.crudao.kanban.workflow.EtapaResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Estado completo do board — etapas × raias × cards (RF-001, RF-006, TASK-04.5).
 *
 * <p>Endpoint mais consultado do sistema (achado do Comitê de Análise — Database, {@code
 * data-model.md} "Nota de performance"): etapas/transições são resolvidas em queries agrupadas
 * (mesmo padrão de {@code WorkflowService.toResponseComEtapas}) e os cards via {@code SELECT NEW}
 * ({@link TarefaRepository#buscarItensDoBoard}) — nenhuma associação {@code lazy} é percorrida em
 * loop, então a contagem de queries não escala com o volume de tarefas.
 */
@Service
public class BoardService {

    private final WorkflowRepository workflowRepository;
    private final EtapaRepository etapaRepository;
    private final TransicaoRepository transicaoRepository;
    private final RaiaRepository raiaRepository;
    private final TarefaRepository tarefaRepository;
    private final PermissaoGuard permissaoGuard;

    public BoardService(
            WorkflowRepository workflowRepository,
            EtapaRepository etapaRepository,
            TransicaoRepository transicaoRepository,
            RaiaRepository raiaRepository,
            TarefaRepository tarefaRepository,
            PermissaoGuard permissaoGuard) {
        this.workflowRepository = workflowRepository;
        this.etapaRepository = etapaRepository;
        this.transicaoRepository = transicaoRepository;
        this.raiaRepository = raiaRepository;
        this.tarefaRepository = tarefaRepository;
        this.permissaoGuard = permissaoGuard;
    }

    public BoardResponse board(UUID projetoId) {
        if (!permissaoGuard.membro(projetoId)) {
            throw new AccessDeniedException("Acesso negado");
        }

        List<EtapaResponse> etapas = etapasDoProjeto(projetoId);
        List<RaiaResponse> raias = raiasDoProjeto(projetoId);
        List<TarefaBoardItemResponse> tarefas = tarefaRepository.buscarItensDoBoard(projetoId);

        return new BoardResponse(etapas, raias, tarefas);
    }

    private List<EtapaResponse> etapasDoProjeto(UUID projetoId) {
        Workflow workflow = workflowRepository.findByProjetoId(projetoId).stream().findFirst().orElse(null);
        if (workflow == null) {
            return List.of();
        }

        List<Etapa> etapasDoWorkflow = etapaRepository.findByWorkflowIdOrderByOrdem(workflow.getId());
        Map<UUID, List<UUID>> transicoesPorOrigem =
                transicaoRepository
                        .findByEtapaOrigemIdIn(etapasDoWorkflow.stream().map(Etapa::getId).toList())
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        t -> t.getEtapaOrigem().getId(),
                                        Collectors.mapping(t -> t.getEtapaDestino().getId(), Collectors.toList())));

        return etapasDoWorkflow.stream()
                .map(
                        etapa ->
                                new EtapaResponse(
                                        etapa.getId(),
                                        etapa.getNome(),
                                        etapa.getOrdem(),
                                        etapa.isEtapaFinal(),
                                        transicoesPorOrigem.getOrDefault(etapa.getId(), List.of())))
                .toList();
    }

    /** Raias do projeto ou, na ausência delas, a raia default global (RN-CB-005). */
    private List<RaiaResponse> raiasDoProjeto(UUID projetoId) {
        List<Raia> raiasDoProjeto = raiaRepository.findByProjetoIdOrderByOrdem(projetoId);
        if (!raiasDoProjeto.isEmpty()) {
            return raiasDoProjeto.stream().map(r -> new RaiaResponse(r.getId(), r.getNome(), r.getOrdem(), false)).toList();
        }
        return raiaRepository.findByProjetoIdIsNull().stream()
                .map(r -> new RaiaResponse(r.getId(), r.getNome(), r.getOrdem(), true))
                .toList();
    }
}
