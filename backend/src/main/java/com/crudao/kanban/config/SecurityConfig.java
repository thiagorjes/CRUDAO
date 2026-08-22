package com.crudao.kanban.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração mínima de segurança para o setup inicial (TASK-00.2): libera o endpoint de saúde e o
 * handshake do WebSocket, exige autenticação OIDC (Keycloak) para o restante.
 *
 * <p>RBAC granular (papéis/permissões da aplicação) é implementado na TASK-04.1 — ver ADR-003.
 */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/health", "/ws/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2Login(withDefaults())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
        .csrf(csrf -> csrf.ignoringRequestMatchers("/ws/**"));
    return http.build();
  }
}
