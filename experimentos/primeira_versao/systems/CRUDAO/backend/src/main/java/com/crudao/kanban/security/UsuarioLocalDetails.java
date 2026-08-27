package com.crudao.kanban.security;

import com.crudao.kanban.domain.rbac.Usuario;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adapta um {@link Usuario} local (login de fallback, ADR-003) ao contrato de autenticação do
 * Spring Security.
 */
public record UsuarioLocalDetails(Usuario usuario) implements UserDetails {

  @Override
  public List<GrantedAuthority> getAuthorities() {
    return List.of(
        new SimpleGrantedAuthority("ROLE_" + usuario.getPapel().getNome().toUpperCase()));
  }

  @Override
  public String getPassword() {
    return usuario.getSenhaHash();
  }

  @Override
  public String getUsername() {
    return usuario.getEmail();
  }
}
