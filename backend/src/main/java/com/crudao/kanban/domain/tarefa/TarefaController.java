package com.crudao.kanban.domain.tarefa;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD e movimentação de Tarefa — RF-002, RF-003, RF-004, RF-012. */
@RestController
@RequestMapping("/api/tarefas")
@RequiredArgsConstructor
public class TarefaController {

  private final TarefaService tarefaService;

  @GetMapping
  public List<TarefaDTO> listarPorProjeto(@RequestParam UUID projetoId) {
    return tarefaService.listarPorProjeto(projetoId);
  }

  @GetMapping("/{id}")
  public TarefaDTO buscar(@PathVariable UUID id) {
    return tarefaService.buscar(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TarefaDTO criar(@Valid @RequestBody TarefaRequest request) {
    return tarefaService.criar(request);
  }

  @PutMapping("/{id}")
  public TarefaDTO editar(@PathVariable UUID id, @Valid @RequestBody TarefaRequest request) {
    return tarefaService.editar(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> excluir(@PathVariable UUID id) {
    tarefaService.excluir(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/mover")
  public TarefaDTO mover(@PathVariable UUID id, @Valid @RequestBody TarefaMoverRequest request) {
    return tarefaService.mover(id, request);
  }

  @PatchMapping("/{id}/mover-projeto")
  public TarefaDTO moverParaProjeto(
      @PathVariable UUID id, @Valid @RequestBody TarefaMoverProjetoRequest request) {
    return tarefaService.moverParaProjeto(id, request);
  }

  @PostMapping("/{id}/impedimento")
  public TarefaDTO marcarImpedimento(
      @PathVariable UUID id, @RequestBody(required = false) TarefaImpedimentoRequest request) {
    return tarefaService.marcarImpedimento(
        id, request != null ? request : new TarefaImpedimentoRequest(null));
  }

  @DeleteMapping("/{id}/impedimento")
  public TarefaDTO desmarcarImpedimento(@PathVariable UUID id) {
    return tarefaService.desmarcarImpedimento(id);
  }

  @GetMapping("/{id}/observadores")
  public List<UUID> listarObservadores(@PathVariable UUID id) {
    return tarefaService.listarObservadores(id);
  }

  @PostMapping("/{id}/observadores/{usuarioId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void adicionarObservador(@PathVariable UUID id, @PathVariable UUID usuarioId) {
    tarefaService.adicionarObservador(id, usuarioId);
  }

  @DeleteMapping("/{id}/observadores/{usuarioId}")
  public ResponseEntity<Void> removerObservador(
      @PathVariable UUID id, @PathVariable UUID usuarioId) {
    tarefaService.removerObservador(id, usuarioId);
    return ResponseEntity.noContent().build();
  }
}
