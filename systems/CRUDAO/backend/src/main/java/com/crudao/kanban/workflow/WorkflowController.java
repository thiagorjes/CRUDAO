package com.crudao.kanban.workflow;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD de {@code Workflow} (RF-002, RF-009, RF-010).
 *
 * <p>Autorização delegada ao {@code WorkflowService} — resolve o projeto a partir do path ou do
 * {@code workflowId} antes de checar {@code workflow:administrar}/RN-015.
 */
@RestController
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/api/projetos/{projetoId}/workflows")
    public List<WorkflowComEtapasResponse> listar(@PathVariable UUID projetoId) {
        return workflowService.listar(projetoId);
    }

    @PostMapping("/api/projetos/{projetoId}/workflows")
    public ResponseEntity<WorkflowResponse> criar(
            @PathVariable UUID projetoId, @RequestBody CriarWorkflowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.criar(projetoId, request));
    }

    @DeleteMapping("/api/workflows/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        workflowService.excluirWorkflow(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/workflows/{id}/etapas")
    public ResponseEntity<EtapaResponse> criarEtapa(@PathVariable UUID id, @RequestBody CriarEtapaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.criarEtapa(id, request));
    }
}
