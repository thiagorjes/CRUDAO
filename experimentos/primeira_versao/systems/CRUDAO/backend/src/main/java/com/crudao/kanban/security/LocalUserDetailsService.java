package com.crudao.kanban.security;

import com.crudao.kanban.domain.rbac.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Autenticação local de fallback (RF-014 Should Have, ADR-003) usada quando o Keycloak está
 * indisponível — apenas usuários com senha local cadastrada ({@code senhaHash} preenchido) podem
 * autenticar por este caminho (HTTP Basic).
 */
@Service
@RequiredArgsConstructor
public class LocalUserDetailsService implements UserDetailsService {

  private final UsuarioRepository usuarioRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return usuarioRepository
        .findByEmailIgnoreCase(email)
        .filter(usuario -> usuario.getSenhaHash() != null)
        .map(UsuarioLocalDetails::new)
        .orElseThrow(() -> new UsernameNotFoundException("Usuário local não encontrado: " + email));
  }
}
