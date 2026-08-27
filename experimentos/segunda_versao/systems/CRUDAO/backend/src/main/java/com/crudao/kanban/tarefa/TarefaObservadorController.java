package com.crudao.kanban.tarefa;

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

/** CRUD de observadores explícitos da tarefa (RF-005, TASK-05.2). */
@RestController
public class TarefaObservadorController {

    private final TarefaObservadorService tarefaObservadorService;

    public TarefaObservadorController(TarefaObservadorService tarefaObservadorService) {
        this.tarefaObservadorService = tarefaObservadorService;
    }

    @GetMapping("/api/tarefas/{id}/observadores")
    public List<UUID> listar(@PathVariable UUID id) {
        return tarefaObservadorService.listar(id);
    }

    @PostMapping("/api/tarefas/{id}/observadores")
    public ResponseEntity<Void> adicionar(@PathVariable UUID id, @RequestBody TarefaObservadorRequest request) {
        tarefaObservadorService.adicionar(id, request.usuarioId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/tarefas/{id}/observadores/{usuarioId}")
    public ResponseEntity<Void> remover(@PathVariable UUID id, @PathVariable UUID usuarioId) {
        tarefaObservadorService.remover(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
