package com.crudao.kanban.workflow;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Edição/exclusão de {@code Etapa} e gestão de {@code Transicao} de saída (RF-002, RF-010, RN-003,
 * RN-005).
 */
@RestController
public class EtapaController {

    private final WorkflowService workflowService;

    public EtapaController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PutMapping("/api/etapas/{id}")
    public EtapaResponse editar(@PathVariable UUID id, @RequestBody EditarEtapaRequest request) {
        return workflowService.editarEtapa(id, request);
    }

    @DeleteMapping("/api/etapas/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        workflowService.excluirEtapa(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/etapas/{id}/transicoes")
    public TransicoesResponse atualizarTransicoes(
            @PathVariable UUID id, @RequestBody AtualizarTransicoesRequest request) {
        return workflowService.atualizarTransicoes(id, request);
    }
}
