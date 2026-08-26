package com.crudao.kanban.auth;

/**
 * Corpo opcional de {@code POST /api/auth/logout} — {@code idTokenHint} é exigido pelo Keycloak
 * para o RP-Initiated Logout quando a sessão não é a do fluxo {@code oauth2Login} do backend (ex.:
 * chamada stateless via Bearer token feita pelo frontend).
 */
public record LogoutRequest(String idTokenHint) {}
