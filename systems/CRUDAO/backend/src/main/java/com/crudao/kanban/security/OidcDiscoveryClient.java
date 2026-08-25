package com.crudao.kanban.security;

/** Verifica disponibilidade do endpoint de discovery OIDC do Keycloak. */
public interface OidcDiscoveryClient {

    /**
     * @throws RuntimeException se o Keycloak não responder com sucesso.
     */
    void verificarDisponibilidade(String issuerUri);
}
