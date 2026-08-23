package com.crudao.kanban.domain.workflow;

import com.crudao.kanban.security.ExigePermissao;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD de Transição entre etapas — RF-002, RF-012 (tipo REABERTURA). */
@RestController
@RequestMapping("/api/transicoes")
@RequiredArgsConstructor
public class TransicaoController {

  private final TransicaoService transicaoService;

  @GetMapping
  public List<TransicaoDTO> listarPorWorkflow(@RequestParam UUID workflowId) {
    return transicaoService.listarPorWorkflow(workflowId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @ExigePermissao("workflow:gerenciar")
  public TransicaoDTO criar(@Valid @RequestBody TransicaoRequest request) {
    return transicaoService.criar(request);
  }

  @DeleteMapping("/{id}")
  @ExigePermissao("workflow:gerenciar")
  public ResponseEntity<Void> excluir(@PathVariable UUID id) {
    transicaoService.excluir(id);
    return ResponseEntity.noContent().build();
  }
}
