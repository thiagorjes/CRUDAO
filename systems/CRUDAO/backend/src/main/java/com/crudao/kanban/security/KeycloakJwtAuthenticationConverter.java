package com.crudao.kanban.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Converte {@code realm_access.roles} do access_token do Keycloak (não do id_token — ver
 * CRUDAO-keycloak-contract.md) em {@link GrantedAuthority} no formato {@code ROLE_<papel>}. A
 * validação de permissão granular (RNF-003) é feita à parte, por {@link PermissaoAspect}.
 */
@Component
public class KeycloakJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    return new JwtAuthenticationToken(jwt, extrairAuthorities(jwt));
  }

  @SuppressWarnings("unchecked")
  private Collection<GrantedAuthority> extrairAuthorities(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaim("realm_access");
    if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> roles)) {
      return List.of();
    }
    return roles.stream()
        .<GrantedAuthority>map(
            role -> new SimpleGrantedAuthority("ROLE_" + role.toString().toUpperCase()))
        .collect(Collectors.toSet());
  }
}
