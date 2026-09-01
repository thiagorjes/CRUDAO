package com.crudao.kanban.security;

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

/**
 * Autenticação via Keycloak (OIDC), sem fallback local (ADR-006).
 *
 * <p>Duas filter chains: {@code /api/**}/{@code /ws/**} como resource server stateless (opaque
 * token/introspection) e o restante (hoje só health/probe) autenticado sem mecanismo de login
 * próprio — o Authorization Code Flow real é todo feito pelo BFF do Next.js (TASK-07.1). O handshake
 * WebSocket usa ticket de curta duração ({@code ?ticket=}, TASK-07.7) por não poder carregar Bearer
 * no upgrade nativo.
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
            com.crudao.kanban.websocket.WsTicketAuthenticationFilter wsTicketAuthenticationFilter)
            throws Exception {
        // /ws/** (handshake STOMP, TASK-05.1/ADR-004) protegido pelo mesmo resource server opaco.
        // O browser não consegue enviar Bearer no handshake nativo de WebSocket (o BFF do frontend
        // nunca expõe o token ao JS) — WsTicketAuthenticationFilter autentica via ticket de curta
        // duração passado como `?ticket=` (TASK-07.7). Não há SockJS (removido em StompConfig), logo
        // nenhum sub-path `/ws/info` a liberar.
        http.securityMatcher("/api/**", "/ws/**")
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/api/ping")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.opaqueToken(Customizer.withDefaults()))
                // Handshake WebSocket: o browser não envia Bearer no upgrade nativo — quando há
                // `?ticket=`, este filtro valida o ticket de curta duração (TASK-07.7) e popula o
                // SecurityContext ANTES do BearerTokenAuthenticationFilter (que, sem Bearer, não
                // autenticaria e a requisição cairia em 401).
                .addFilterBefore(
                        wsTicketAuthenticationFilter, BearerTokenAuthenticationFilter.class)
                // addFilterAfter(UsernamePasswordAuthenticationFilter.class) posicionava ANTES do
                // slot de BearerTokenAuthenticationFilter na ordem canônica do Spring Security
                // (achado de execução real, TASK-08.3 — primeira vez que uma chamada HTTP
                // autenticada de verdade passou pela cadeia real de filtros; nenhum teste unitário
                // ou @WebMvcTest exercitava esse caminho). AtivoUsuarioFilter lê
                // SecurityContextHolder.getAuthentication() esperando a autenticação já resolvida
                // pelo resource server — rodando antes, `UsuarioAutenticadoHolder` nunca era
                // setado e toda chamada autenticada real caía em 403 (AccessDeniedException) nos
                // services que dependem dele.
                .addFilterAfter(ativoUsuarioFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Chain para o que sobra fora de {@code /api/**}/{@code /ws/**} — hoje só os endpoints de
     * health/probe. Não tem mais {@code oauth2Login} (achado de execução real, TASK-08.3): era
     * config morta desde TASK-07.1 (o Next.js BFF assumiu o Authorization Code Flow), mas o
     * client-registration que ela exigia era resolvido eagerly no boot via discovery OIDC,
     * incompatível com o Keycloak atrás de hostnames diferentes para browser/containers — sem
     * ela, {@code KC_HOSTNAME} pode ser fixado no Keycloak (docker-compose.yml) sem quebrar o
     * boot do backend, o que é necessário para o resource server (introspection) validar
     * corretamente tokens emitidos pelo fluxo do browser.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain browserFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(
                auth ->
                        auth.requestMatchers("/actuator/health/**")
                                .permitAll()
                                // demais endpoints do actuator (incl. /actuator/health com
                                // show-details=always) exigem sessão autenticada — não podem
                                // expor detalhes internos (ex.: stacktrace do health-check do
                                // Keycloak) sem autenticação. Sem oauth2Login, não há mecanismo de
                                // login aqui — cai no 403 default do Spring Security.
                                .anyRequest()
                                .authenticated());
        return http.build();
    }
}
