package com.crudao.kanban.security;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.auth.UsuarioProvisioningService;
import com.crudao.kanban.domain.usuario.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Roda após a autenticação do resource server em {@code /api/**} (JWT ou opaque-token/introspection
 * — {@link AbstractOAuth2TokenAuthenticationToken} cobre ambos): resolve/provisiona o {@link
 * Usuario} local a partir do {@code sub} do token (JIT) e bloqueia com 401 quando {@code
 * ativo=false} — mesmo com token válido (achado do Comitê — Security, TASK-02.1).
 */
@Component
public class AtivoUsuarioFilter extends OncePerRequestFilter {

    private final UsuarioProvisioningService provisioningService;

    public AtivoUsuarioFilter(UsuarioProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof AbstractOAuth2TokenAuthenticationToken<?> tokenAuth) {
            Map<String, Object> claims = tokenAuth.getTokenAttributes();
            String sub = String.valueOf(claims.get("sub"));
            String nome = claimOuVazio(claims, "name");
            String email = claimOuVazio(claims, "email");

            Usuario usuario = provisioningService.provisionar(sub, nome, email);

            if (!usuario.isAtivo()) {
                responderNaoAutorizado(response, "usuario_inativo");
                return;
            }

            UsuarioAutenticadoHolder.set(usuario);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UsuarioAutenticadoHolder.clear();
        }
    }

    private static String claimOuVazio(Map<String, Object> claims, String claim) {
        Object valor = claims.get(claim);
        return valor != null ? valor.toString() : "";
    }

    private static void responderNaoAutorizado(HttpServletResponse response, String motivo)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"%s\"}".formatted(motivo));
    }
}
