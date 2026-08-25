package com.crudao.kanban.security;

import com.crudao.kanban.auth.UsuarioProvisioningService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Dispara o provisioning JIT (RF-014) no primeiro login bem-sucedido via Keycloak e redireciona ao
 * frontend, conforme contrato de autenticação.
 */
@Component
public class OidcLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UsuarioProvisioningService provisioningService;
    private final SimpleUrlAuthenticationSuccessHandler redirectHandler;

    public OidcLoginSuccessHandler(
            UsuarioProvisioningService provisioningService,
            @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.provisioningService = provisioningService;
        this.redirectHandler = new SimpleUrlAuthenticationSuccessHandler(frontendUrl + "/projetos");
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, jakarta.servlet.ServletException {
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            provisioningService.provisionar(
                    oidcUser.getSubject(), oidcUser.getFullName(), oidcUser.getEmail());
        }
        redirectHandler.onAuthenticationSuccess(request, response, authentication);
    }
}
