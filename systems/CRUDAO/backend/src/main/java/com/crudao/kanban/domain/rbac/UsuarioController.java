package com.crudao.kanban.domain.rbac;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Listagem somente-leitura de usuários — usada pelo board para resolver {@code responsavelId} em
 * nome/inicial (RF-001) e para o seletor de observadores (RF-005). Sem endpoint de escrita: o
 * cadastro de usuário é feito via auto-provisionamento no primeiro login (Keycloak) ou por seed
 * (login local de fallback, ADR-003) — RBAC/edição de papel fica em {@link PapelController}.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

  private final UsuarioRepository usuarioRepository;

  @GetMapping
  public List<UsuarioDTO> listar() {
    return usuarioRepository.findAll().stream()
        .map(u -> new UsuarioDTO(u.getId(), u.getNome()))
        .toList();
  }
}
