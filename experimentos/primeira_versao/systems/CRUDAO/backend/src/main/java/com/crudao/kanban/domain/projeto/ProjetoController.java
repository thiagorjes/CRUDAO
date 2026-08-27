package com.crudao.kanban.domain.projeto;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD de Projeto — RF-008. */
@RestController
@RequestMapping("/api/projetos")
@RequiredArgsConstructor
public class ProjetoController {

  private final ProjetoService projetoService;

  @GetMapping
  public List<ProjetoDTO> listar() {
    return projetoService.listar();
  }

  @GetMapping("/{id}")
  public ProjetoDTO buscar(@PathVariable UUID id) {
    return projetoService.buscar(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProjetoDTO criar(@Valid @RequestBody ProjetoRequest request) {
    return projetoService.criar(request);
  }

  @PutMapping("/{id}")
  public ProjetoDTO editar(@PathVariable UUID id, @Valid @RequestBody ProjetoRequest request) {
    return projetoService.editar(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> excluir(@PathVariable UUID id) {
    projetoService.excluir(id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{id}/workflow-ativo")
  public ResponseEntity<Void> definirWorkflowAtivo(
      @PathVariable UUID id, @Valid @RequestBody DefinirWorkflowAtivoRequest request) {
    projetoService.definirWorkflowAtivo(id, request.workflowId());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/configuracao")
  public ConfiguracaoProjetoDTO buscarConfiguracao(@PathVariable UUID id) {
    return projetoService.buscarConfiguracao(id);
  }

  @PutMapping("/{id}/configuracao")
  public ConfiguracaoProjetoDTO atualizarConfiguracao(
      @PathVariable UUID id, @Valid @RequestBody ConfiguracaoProjetoDTO request) {
    return projetoService.atualizarConfiguracao(id, request);
  }

  @PutMapping("/{id}/finalizar")
  public ResponseEntity<Void> finalizar(@PathVariable UUID id) {
    projetoService.finalizar(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}/finalizar")
  public ResponseEntity<Void> reabrir(@PathVariable UUID id) {
    projetoService.reabrir(id);
    return ResponseEntity.noContent().build();
  }
}
