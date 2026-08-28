package com.crudao.kanban.workflow;

import com.crudao.kanban.workflow.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping("/api/projetos/{projetoId}/workflows")
    public ResponseEntity<List<WorkflowResponse>> listarWorkflows(@PathVariable UUID projetoId) {
        return ResponseEntity.ok(workflowService.listarWorkflows(projetoId));
    }

    @PostMapping("/api/projetos/{projetoId}/workflows")
    public ResponseEntity<WorkflowResponse> criarWorkflow(
            @PathVariable UUID projetoId,
            @Valid @RequestBody CriarWorkflowRequest request) {
        WorkflowResponse resp = workflowService.criarWorkflow(projetoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @DeleteMapping("/api/workflows/{id}")
    public ResponseEntity<Void> excluirWorkflow(@PathVariable UUID id) {
        workflowService.excluirWorkflow(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/workflows/{id}/etapas")
    public ResponseEntity<EtapaResponse> criarEtapa(
            @PathVariable UUID id,
            @Valid @RequestBody CriarEtapaRequest request) {
        EtapaResponse resp = workflowService.criarEtapa(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }
}

