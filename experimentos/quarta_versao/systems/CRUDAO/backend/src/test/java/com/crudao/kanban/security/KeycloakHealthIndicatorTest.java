package com.crudao.kanban.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;

/** Health-check dedicado de dependência Keycloak (TASK-02.1). */
@ExtendWith(MockitoExtension.class)
class KeycloakHealthIndicatorTest {

    private static final String ISSUER_URI = "http://localhost:8080/realms/kanban-dev";

    @Mock private OidcDiscoveryClient discoveryClient;

    @Test
    void quandoKeycloakResponde_statusEhUp() {
        doNothing().when(discoveryClient).verificarDisponibilidade(ISSUER_URI);
        KeycloakHealthIndicator indicator = new KeycloakHealthIndicator(discoveryClient, ISSUER_URI);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(org.springframework.boot.actuate.health.Status.UP);
    }

    @Test
    void quandoKeycloakIndisponivel_statusEhDown() {
        doThrow(new IllegalStateException("indisponível"))
                .when(discoveryClient)
                .verificarDisponibilidade(ISSUER_URI);
        KeycloakHealthIndicator indicator = new KeycloakHealthIndicator(discoveryClient, ISSUER_URI);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(org.springframework.boot.actuate.health.Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }
}
