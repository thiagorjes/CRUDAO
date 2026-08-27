package com.crudao.kanban.domain.rbac;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Associação de usuários a projetos com papéis — RF-015. */
@RestController
@RequestMapping("/api/projetos/{projetoId}/membros")
@RequiredArgsConstructor
public class MembroProjetoController {

  private final MembroProjetoService membroProjetoService;

  @GetMapping
  public List<MembroDTO> listar(@PathVariable UUID projetoId) {
    return membroProjetoService.listar(projetoId);
  }

  @PutMapping("/{usuarioId}")
  public ResponseEntity<Void> atribuir(
      @PathVariable UUID projetoId,
      @PathVariable UUID usuarioId,
      @Valid @RequestBody AtribuirPapeisRequest request) {
    membroProjetoService.atribuir(projetoId, usuarioId, request);
    return ResponseEntity.noContent().build();
  }
}
