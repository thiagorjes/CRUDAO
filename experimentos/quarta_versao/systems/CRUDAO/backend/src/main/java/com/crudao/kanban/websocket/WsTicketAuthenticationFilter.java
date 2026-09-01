package com.crudao.kanban.websocket;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica o handshake WebSocket via {@code ?ticket=} de curta duração (TASK-07.7).
 *
 * <p>Só age em requisições para {@code /ws**} que tragam o parâmetro {@code ticket}. Valida a
 * assinatura/expiração via {@link WsTicketService} e popula o {@code SecurityContext} com uma
 * autenticação cujo {@code name} é o e-mail do usuário — é esse {@code Principal} que o
 * {@link BoardChannelInterceptor} usa para autorizar as subscrições STOMP.
 *
 * <p>Ticket ausente: o filtro não faz nada (a cadeia segue e o resource server nega com 401).
 * Ticket inválido: responde 401 imediatamente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsTicketAuthenticationFilter extends OncePerRequestFilter {

    private static final SimpleGrantedAuthority ROLE_WS =
            new SimpleGrantedAuthority("ROLE_WS_CLIENT");

    private final WsTicketService wsTicketService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/ws") || request.getParameter("ticket") == null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Optional<String> email = wsTicketService.validar(request.getParameter("ticket"));
        if (email.isEmpty()) {
            log.warn("Handshake WS recusado: ticket inválido ou expirado ({})", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        var auth =
                new UsernamePasswordAuthenticationToken(email.get(), null, List.of(ROLE_WS));
        SecurityContextHolder.getContext().setAuthentication(auth);
        log.debug("Handshake WS autenticado via ticket para {}", email.get());

        filterChain.doFilter(request, response);
    }
}
