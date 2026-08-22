package com.crudao.kanban.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração mínima de segurança para o setup inicial (TASK-00.2): libera o endpoint de saúde, o
 * handshake do WebSocket e os endpoints REST de domínio (`/api/**`) enquanto o RBAC granular
 * (papéis/permissões da aplicação) ainda não foi implementado.
 *
 * <p><b>TODO(TASK-04.1):</b> substituir `permitAll()` em `/api/**` pela validação de permissão por
 * papel (RNF-003) — ver ADR-003. Autenticação OIDC continua exigida para o restante.
 */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/**", "/ws/**").permitAll().anyRequest().authenticated())
        .oauth2Login(withDefaults())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
        // TODO(TASK-04.1): revisar estratégia de CSRF junto com a definição final de auth/RBAC.
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/ws/**"));
    return http.build();
  }
}
