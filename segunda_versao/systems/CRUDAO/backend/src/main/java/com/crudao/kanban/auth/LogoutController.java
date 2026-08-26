package com.crudao.kanban.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@code POST /api/auth/logout} — encerra a sessão local e dispara RP-Initiated Logout no
 * Keycloak (back-channel), invalidando o token no IdP e não apenas a sessão local (achado do
 * Comitê de Análise — Security, ADR-006).
 *
 * <p>Como o resource server valida o access token via <b>introspection</b> (ver {@link
 * com.crudao.kanban.security.SecurityConfig}), revogar o token aqui (RFC 7009) faz o próximo
 * request com o mesmo Bearer ser rejeitado imediatamente — não depende de esperar a expiração do
 * JWT.
 *
 * <p>Chamadas ao Keycloak são best-effort: se o IdP estiver indisponível no momento do logout, a
 * sessão/local já foi encerrada e o request ainda responde {@code 204} (indisponibilidade do
 * Keycloak não deve travar o usuário sem conseguir sair da aplicação) — a checagem estrita de
 * "sem fallback" (ADR-006) se aplica à autenticação, não ao logout.
 */
@RestController
public class LogoutController {

    private static final Logger log = LoggerFactory.getLogger(LogoutController.class);

    private final RestClient restClient = RestClient.create();
    private final String issuerUri;
    private final String clientId;
    private final String clientSecret;

    public LogoutController(
            @Value("${app.keycloak.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id}") String clientId,
            @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-secret}")
                    String clientSecret) {
        this.issuerUri = issuerUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request, @RequestBody(required = false) LogoutRequest body) {
        String accessToken = extrairBearerToken(request);
        String idTokenHint = resolverIdTokenHint(body);

        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        SecurityContextHolder.clearContext();

        if (accessToken != null) {
            revogarToken(accessToken);
        }
        if (idTokenHint != null && !idTokenHint.isBlank()) {
            encerrarSessaoNoKeycloak(idTokenHint);
        }

        return ResponseEntity.noContent().build();
    }

    /** RFC 7009 — revoga o access token no Keycloak para invalidar a introspection imediatamente. */
    private void revogarToken(String accessToken) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("token", accessToken);
            form.add("token_type_hint", "access_token");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);

            restClient
                    .post()
                    .uri(issuerUri + "/protocol/openid-connect/revoke")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Falha ao revogar access token no Keycloak (best-effort) — {}", e.getMessage());
        }
    }

    private void encerrarSessaoNoKeycloak(String idTokenHint) {
        try {
            restClient
                    .get()
                    .uri(
                            issuerUri
                                    + "/protocol/openid-connect/logout?id_token_hint={idTokenHint}",
                            idTokenHint)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Falha no RP-Initiated Logout no Keycloak (best-effort) — {}", e.getMessage());
        }
    }

    private static String extrairBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return header.substring(7);
        }
        return null;
    }

    private static String resolverIdTokenHint(LogoutRequest body) {
        if (body != null && body.idTokenHint() != null) {
            return body.idTokenHint();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            OidcIdToken idToken = oidcUser.getIdToken();
            return idToken != null ? idToken.getTokenValue() : null;
        }
        return null;
    }
}
