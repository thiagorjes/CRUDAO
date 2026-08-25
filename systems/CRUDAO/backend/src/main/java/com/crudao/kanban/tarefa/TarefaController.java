package com.crudao.kanban.tarefa;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Criação, movimentação, edição e detalhe de card (RF-018, RF-002, RF-003, RF-006, RF-012).
 *
 * <p>Autorização delegada ao {@code TarefaService} — ver {@code contracts/tarefas.md}.
 */
@RestController
public class TarefaController {

    private final TarefaService tarefaService;

    public TarefaController(TarefaService tarefaService) {
        this.tarefaService = tarefaService;
    }

    @PostMapping("/api/projetos/{projetoId}/tarefas")
    public ResponseEntity<TarefaResponse> criar(
            @PathVariable UUID projetoId, @RequestBody CriarTarefaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tarefaService.criar(projetoId, request));
    }

    @PostMapping("/api/tarefas/{id}/mover")
    public ResponseEntity<TarefaResponse> mover(@PathVariable UUID id, @RequestBody MoverTarefaRequest request) {
        return ResponseEntity.ok(tarefaService.mover(id, request));
    }

    @PutMapping("/api/tarefas/{id}")
    public ResponseEntity<TarefaResponse> editar(@PathVariable UUID id, @RequestBody EditarTarefaRequest request) {
        return ResponseEntity.ok(tarefaService.editar(id, request));
    }

    @GetMapping("/api/tarefas/{id}")
    public ResponseEntity<TarefaDetalheResponse> detalhe(@PathVariable UUID id) {
        return ResponseEntity.ok(tarefaService.detalhe(id));
    }

    @PostMapping("/api/tarefas/{id}/impedimento")
    public ResponseEntity<TarefaResponse> marcarImpedimento(@PathVariable UUID id) {
        return ResponseEntity.ok(tarefaService.marcarImpedimento(id));
    }

    @DeleteMapping("/api/tarefas/{id}/impedimento")
    public ResponseEntity<TarefaResponse> desmarcarImpedimento(@PathVariable UUID id) {
        return ResponseEntity.ok(tarefaService.desmarcarImpedimento(id));
    }
}
