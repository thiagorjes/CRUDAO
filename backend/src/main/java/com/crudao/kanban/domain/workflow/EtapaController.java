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

/** CRUD de Etapa (coluna) — RF-010. */
@RestController
@RequestMapping("/api/etapas")
@RequiredArgsConstructor
public class EtapaController {

  private final EtapaService etapaService;

  @GetMapping
  public List<EtapaDTO> listarPorWorkflow(@RequestParam UUID workflowId) {
    return etapaService.listarPorWorkflow(workflowId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EtapaDTO criar(@Valid @RequestBody EtapaRequest request) {
    return etapaService.criar(request);
  }

  @PutMapping("/{id}")
  public EtapaDTO editar(@PathVariable UUID id, @Valid @RequestBody EtapaRequest request) {
    return etapaService.editar(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> excluir(@PathVariable UUID id) {
    etapaService.excluir(id);
    return ResponseEntity.noContent().build();
  }
}
