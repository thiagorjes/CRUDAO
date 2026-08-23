package com.crudao.kanban.config;

import static org.springframework.security.config.Customizer.withDefaults;

import com.crudao.kanban.security.KeycloakJwtAuthenticationConverter;
import com.crudao.kanban.security.LocalUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança do RBAC híbrido (RF-013, RF-014, RNF-003, ADR-003, TASK-04.1):
 * autenticação via Keycloak (OIDC/JWT) com fallback de login local via HTTP Basic quando o Keycloak
 * está indisponível. Autorização granular (papel/permissão) é validada à parte, em
 * {@code @ExigePermissao} nos endpoints de escrita — aqui apenas exige-se autenticação.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final LocalUserDetailsService localUserDetailsService;
  private final KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/ws/**", "/api/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .userDetailsService(localUserDetailsService)
        .httpBasic(withDefaults())
        .oauth2Login(withDefaults())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(
                    jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter)))
        // CSRF desabilitado para API stateless (tokens Bearer/Basic) e handshake STOMP.
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/ws/**"));
    return http.build();
  }
}
