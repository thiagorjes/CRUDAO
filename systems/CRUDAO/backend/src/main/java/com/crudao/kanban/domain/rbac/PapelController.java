package com.crudao.kanban.domain.rbac;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD de Papel/Permissão do RBAC interno — RF-013. */
@RestController
@RequestMapping("/api/papeis")
@RequiredArgsConstructor
public class PapelController {

  private final PapelService papelService;

  @GetMapping
  public List<PapelDTO> listar() {
    return papelService.listar();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @ExigePermissao("papel:gerenciar")
  public PapelDTO criar(@Valid @RequestBody PapelRequest request) {
    return papelService.criar(request);
  }

  @PutMapping("/{id}")
  @ExigePermissao("papel:gerenciar")
  public PapelDTO editar(@PathVariable UUID id, @Valid @RequestBody PapelRequest request) {
    return papelService.editar(id, request);
  }

  @DeleteMapping("/{id}")
  @ExigePermissao("papel:gerenciar")
  public ResponseEntity<Void> excluir(@PathVariable UUID id) {
    papelService.excluir(id);
    return ResponseEntity.noContent().build();
  }
}
