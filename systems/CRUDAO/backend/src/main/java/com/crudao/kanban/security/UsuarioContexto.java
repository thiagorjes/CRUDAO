package com.crudao.kanban.security;

import com.crudao.kanban.common.AcessoNegadoException;
import com.crudao.kanban.domain.rbac.Papel;
import com.crudao.kanban.domain.rbac.PapelRepository;
import com.crudao.kanban.domain.rbac.Usuario;
import com.crudao.kanban.domain.rbac.UsuarioRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolve o {@link Usuario} interno correspondente ao autenticado na requisição atual, seja via
 * Keycloak (JWT — RF-014) ou via login local de fallback (ADR-003). Auto-provisiona o usuário no
 * primeiro acesso via Keycloak, mapeando o papel a partir de {@code realm_access.roles}.
 */
@Component
@RequiredArgsConstructor
public class UsuarioContexto {

  private static final String PAPEL_PADRAO = "user";

  private final UsuarioRepository usuarioRepository;
  private final PapelRepository papelRepository;

  @Transactional
  public Usuario usuarioAtual() {
    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacao == null || !autenticacao.isAuthenticated()) {
      throw new AcessoNegadoException("Nenhum usuário autenticado.");
    }
    if (autenticacao.getPrincipal() instanceof Jwt jwt) {
      return resolverPorJwt(jwt);
    }
    if (autenticacao.getPrincipal() instanceof UsuarioLocalDetails local) {
      return local.usuario();
    }
    throw new AcessoNegadoException("Tipo de autenticação não suportado.");
  }

  private Usuario resolverPorJwt(Jwt jwt) {
    return usuarioRepository.findByKeycloakSub(jwt.getSubject()).orElseGet(() -> provisionar(jwt));
  }

  private Usuario provisionar(Jwt jwt) {
    Papel papel =
        papelRepository
            .findByNomeIgnoreCase(papelDoToken(jwt))
            .orElseThrow(
                () ->
                    new AcessoNegadoException(
                        "Nenhum papel configurado corresponde às roles do usuário autenticado."));
    Usuario usuario = new Usuario();
    usuario.setKeycloakSub(jwt.getSubject());
    usuario.setEmail(jwt.getClaimAsString("email"));
    usuario.setNome(jwt.getClaimAsString("name"));
    usuario.setPapel(papel);
    // BDR-001: role 'admin' do Keycloak vira o papel global Usuario.admin. Demais papéis não geram
    // nenhum UsuarioProjetoPapel automático — associação a projeto é sempre manual (RF-015,
    // RN-014).
    usuario.setAdmin("admin".equalsIgnoreCase(papel.getNome()));
    return usuarioRepository.save(usuario);
  }

  /**
   * Lê {@code realm_access.roles} do access_token (não do id_token — ver nota no contrato
   * CRUDAO-keycloak-contract.md) e retorna a primeira role que corresponde a um papel configurado;
   * caso nenhuma corresponda, usa o papel padrão {@code user}.
   */
  @SuppressWarnings("unchecked")
  private String papelDoToken(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaim("realm_access");
    if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> roles)) {
      return PAPEL_PADRAO;
    }
    return roles.stream()
        .map(Object::toString)
        .filter(role -> papelRepository.findByNomeIgnoreCase(role).isPresent())
        .findFirst()
        .orElse(PAPEL_PADRAO);
  }
}
