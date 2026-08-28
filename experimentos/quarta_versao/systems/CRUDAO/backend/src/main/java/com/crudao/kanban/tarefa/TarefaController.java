package com.crudao.kanban.tarefa;

import com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import com.crudao.kanban.tarefa.dto.CriarTarefaResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TarefaController {

    private final TarefaService tarefaService;

    @PostMapping("/api/projetos/{projetoId}/tarefas")
    public ResponseEntity<CriarTarefaResponse> criarTarefa(
            @PathVariable UUID projetoId,
            @Valid @RequestBody CriarTarefaRequest request) {
        CriarTarefaResponse resp = tarefaService.criarTarefa(projetoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }
}

