package com.crudao.kanban.workflow;

import com.crudao.kanban.workflow.dto.*;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/etapas")
@RequiredArgsConstructor
public class EtapaController {

    private final WorkflowService workflowService;

    @PutMapping("/{id}")
    public ResponseEntity<EtapaResponse> atualizarEtapa(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarEtapaRequest request) {
        return ResponseEntity.ok(workflowService.atualizarEtapa(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirEtapa(@PathVariable UUID id) {
        workflowService.excluirEtapa(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/transicoes")
    public ResponseEntity<TransicoesResponse> atualizarTransicoes(
            @PathVariable UUID id,
            @RequestBody AtualizarTransicoesRequest request) {
        return ResponseEntity.ok(workflowService.atualizarTransicoes(id, request));
    }
}

