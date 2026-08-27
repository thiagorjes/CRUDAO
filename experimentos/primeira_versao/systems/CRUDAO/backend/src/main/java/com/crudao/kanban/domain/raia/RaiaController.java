package com.crudao.kanban.domain.raia;

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

/** CRUD de Raia (swimlane) — RF-011. */
@RestController
@RequestMapping("/api/raias")
@RequiredArgsConstructor
public class RaiaController {

  private final RaiaService raiaService;

  @GetMapping
  public List<RaiaDTO> listarParaProjeto(@RequestParam UUID projetoId) {
    return raiaService.listarParaProjeto(projetoId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public RaiaDTO criar(@Valid @RequestBody RaiaRequest request) {
    return raiaService.criar(request);
  }

  @PutMapping("/{id}")
  public RaiaDTO editar(@PathVariable UUID id, @Valid @RequestBody RaiaRequest request) {
    return raiaService.editar(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> excluir(@PathVariable UUID id) {
    raiaService.excluir(id);
    return ResponseEntity.noContent().build();
  }
}
