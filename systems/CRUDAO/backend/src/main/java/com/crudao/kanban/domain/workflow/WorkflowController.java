package com.crudao.kanban.domain.workflow;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD de Workflow — RF-009. */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

  private final WorkflowService workflowService;

  @GetMapping
  public List<WorkflowDTO> listarPorProjeto(@RequestParam UUID projetoId) {
    return workflowService.listarPorProjeto(projetoId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public WorkflowDTO criar(@Valid @RequestBody WorkflowRequest request) {
    return workflowService.criar(request);
  }

  @PutMapping("/{id}")
  public WorkflowDTO editar(@PathVariable UUID id, @Valid @RequestBody WorkflowRequest request) {
    return workflowService.editar(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> excluir(@PathVariable UUID id) {
    workflowService.excluir(id);
    return ResponseEntity.noContent().build();
  }

  /** RN-003: valida se todas as etapas não-finais têm ao menos uma transição de saída. */
  @GetMapping("/{id}/consistencia")
  public ResponseEntity<List<String>> validarConsistencia(@PathVariable UUID id) {
    return ResponseEntity.ok(workflowService.validarConsistencia(id));
  }
}
