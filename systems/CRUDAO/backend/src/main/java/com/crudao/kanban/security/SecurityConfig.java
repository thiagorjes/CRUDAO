package com.crudao.kanban.security;

import com.crudao.kanban.websocket.WsTicketAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Autenticação via Keycloak (OIDC), sem fallback local (ADR-006).
 *
 * <p>Duas filter chains: {@code /api/**} como resource server stateless e o restante com {@code
 * oauth2Login} para o fluxo de Authorization Code (login via browser).
 *
 * <p>O resource server valida o Bearer token via <b>introspection (opaque token)</b>, não via
 * decodificação local de JWT — isso garante que {@code POST /api/auth/logout} revogando o token no
 * Keycloak (achado do Comitê — Security) tenha efeito imediato: a próxima chamada com o mesmo
 * token recebe {@code inactive=true} do endpoint de introspection e cai em 401, em vez de
 * continuar válido até a expiração natural do JWT.
 *
 * <p>{@code @EnableMethodSecurity} habilita {@code @PreAuthorize} nos controllers de escrita das
 * demais epics, usando {@code @permissaoGuard.permitido(...)} (RBAC — TASK-02.2, RNF-003).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(
            HttpSecurity http,
            AtivoUsuarioFilter ativoUsuarioFilter,
            WsTicketAuthenticationFilter wsTicketAuthenticationFilter)
            throws Exception {
        // /ws/** (handshake STOMP, TASK-05.1/ADR-004) protegido pelo mesmo resource server opaco —
        // o AtivoUsuarioFilter resolve o Usuario local no handshake, capturado por
        // AutenticacaoHandshakeInterceptor para uso na sessão WebSocket. O browser não consegue
        // enviar Bearer no handshake nativo de WebSocket (BFF do frontend nunca expõe o token ao
        // JS) — WsTicketAuthenticationFilter autentica via ticket de curta duração nesse caso
        // (TASK-07.2); /ws/info fica fora da autenticação porque só expõe capacidades de
        // transporte do SockJS, sem dado sensível.
        http.securityMatcher("/api/**", "/ws/**")
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/api/ping", "/ws/info")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.opaqueToken(Customizer.withDefaults()))
                .addFilterBefore(wsTicketAuthenticationFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(ativoUsuarioFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain browserFilterChain(
            HttpSecurity http, OidcLoginSuccessHandler oidcLoginSuccessHandler) throws Exception {
        http.authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/actuator/health/liveness",
                                                "/actuator/health/readiness",
                                                "/oauth2/**",
                                                "/login/**")
                                        .permitAll()
                                        // demais endpoints do actuator (incl. /actuator/health com
                                        // show-details=always) exigem sessão autenticada — não
                                        // podem expor detalhes internos (ex.: stacktrace do
                                        // health-check do Keycloak) sem autenticação.
                                        .anyRequest()
                                        .authenticated())
                .oauth2Login(oauth2 -> oauth2.successHandler(oidcLoginSuccessHandler));
        return http.build();
    }
}
