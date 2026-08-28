package com.crudao.kanban.security;

import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementação via {@link RestClient} — usada pelo {@link KeycloakHealthIndicator}.
 *
 * <p>{@code SimpleClientHttpRequestFactory} (não {@code ClientHttpRequestFactoryBuilder}, que só
 * existe a partir do Spring Framework 7/Boot 4) — API compatível com o Boot 3.5.16 pinado em
 * {@code stack.md}.
 */
@Component
public class RestClientOidcDiscoveryClient implements OidcDiscoveryClient {

    private static final String DISCOVERY_PATH = "/.well-known/openid-configuration";

    private final RestClient restClient;

    public RestClientOidcDiscoveryClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public void verificarDisponibilidade(String issuerUri) {
        restClient.get().uri(issuerUri + DISCOVERY_PATH).retrieve().toBodilessEntity();
    }
}
