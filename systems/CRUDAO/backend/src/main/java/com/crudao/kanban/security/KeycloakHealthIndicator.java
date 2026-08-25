package com.crudao.kanban.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health-check dedicado da dependência Keycloak — {@code /actuator/health/keycloak} reflete
 * indisponibilidade do IdP (critério de aceite da TASK-02.1, sem fallback local — ADR-006).
 */
@Component("keycloak")
public class KeycloakHealthIndicator implements HealthIndicator {

    private final OidcDiscoveryClient discoveryClient;
    private final String issuerUri;

    public KeycloakHealthIndicator(
            OidcDiscoveryClient discoveryClient,
            @Value("${app.keycloak.issuer-uri}") String issuerUri) {
        this.discoveryClient = discoveryClient;
        this.issuerUri = issuerUri;
    }

    @Override
    public Health health() {
        try {
            discoveryClient.verificarDisponibilidade(issuerUri);
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}
