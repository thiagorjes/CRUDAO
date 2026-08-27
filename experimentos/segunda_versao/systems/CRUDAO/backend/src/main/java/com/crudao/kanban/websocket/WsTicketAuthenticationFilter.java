package com.crudao.kanban.websocket;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.usuario.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica o handshake HTTP de {@code /ws/**} via o ticket de curta duração (TASK-07.2) quando o
 * request carrega {@code ?ticket=...} — a WebSocket API nativa do browser não permite enviar
 * header {@code Authorization}, então o Bearer normal do resource server não chega até aqui (ver
 * {@code memory/state.md} para a decisão de arquitetura, validada com architect+security).
 *
 * <p>Roda antes do {@code BearerTokenAuthenticationFilter} — se não houver {@code ticket} no
 * request (ex.: qualquer chamada a {@code /api/**}), não faz nada e o fluxo Bearer normal segue
 * intacto. {@code /ws/info} fica de fora (permitAll em {@link
 * com.crudao.kanban.security.SecurityConfig}) porque o SockJS o chama antes de qualquer ticket
 * estar necessariamente disponível para reenvio e ele não expõe dado sensível — só capacidades de
 * transporte.
 */
@Component
public class WsTicketAuthenticationFilter extends OncePerRequestFilter {

    private final WsTicketService wsTicketService;

    public WsTicketAuthenticationFilter(WsTicketService wsTicketService) {
        this.wsTicketService = wsTicketService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isHandshakeWs(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ticketBruto = request.getParameter("ticket");
        if (ticketBruto == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<Usuario> usuario = validar(ticketBruto);
        if (usuario.isEmpty() || !usuario.get().isAtivo()) {
            responderNaoAutorizado(response);
            return;
        }

        var authentication =
                new UsernamePasswordAuthenticationToken(usuario.get().getId(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UsuarioAutenticadoHolder.set(usuario.get());

        filterChain.doFilter(request, response);
    }

    private Optional<Usuario> validar(String ticketBruto) {
        try {
            return wsTicketService.validarEUsar(UUID.fromString(ticketBruto));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static boolean isHandshakeWs(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return (uri.equals("/ws") || uri.startsWith("/ws/")) && !uri.equals("/ws/info");
    }

    private static void responderNaoAutorizado(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"ticket_invalido\"}");
    }
}
