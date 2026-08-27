import { describe, expect, it } from 'vitest';
import { carregarAuthConfig, urlAutorizacao, urlLogout, urlToken } from './config';

describe('carregarAuthConfig', () => {
  it('usa a mesma URL do Keycloak para server e browser quando KEYCLOAK_ISSUER_URI_PUBLIC não é definida', () => {
    const config = carregarAuthConfig({
      KEYCLOAK_ISSUER_URI: 'http://keycloak:8080/realms/crudao',
    });

    expect(config.issuerUriServer).toBe('http://keycloak:8080/realms/crudao');
    expect(config.issuerUriPublic).toBe('http://keycloak:8080/realms/crudao');
  });

  it('permite URLs distintas para server (rede interna) e browser (host)', () => {
    const config = carregarAuthConfig({
      KEYCLOAK_ISSUER_URI: 'http://keycloak:8080/realms/crudao',
      KEYCLOAK_ISSUER_URI_PUBLIC: 'http://localhost:8081/realms/crudao',
    });

    expect(config.issuerUriServer).toBe('http://keycloak:8080/realms/crudao');
    expect(config.issuerUriPublic).toBe('http://localhost:8081/realms/crudao');
  });
});

describe('urlAutorizacao', () => {
  it('monta a URL de /authorize usando a base pública e o redirect_uri do callback', () => {
    const config = carregarAuthConfig({
      KEYCLOAK_ISSUER_URI_PUBLIC: 'http://localhost:8081/realms/crudao',
      APP_BASE_URL: 'http://localhost:3000',
    });

    const url = new URL(urlAutorizacao(config, 'abc123'));

    expect(url.origin + url.pathname).toBe(
      'http://localhost:8081/realms/crudao/protocol/openid-connect/auth',
    );
    expect(url.searchParams.get('redirect_uri')).toBe('http://localhost:3000/api/auth/callback');
    expect(url.searchParams.get('state')).toBe('abc123');
    expect(url.searchParams.get('response_type')).toBe('code');
  });
});

describe('urlToken', () => {
  it('monta a URL de troca de token usando a base server (rede interna)', () => {
    const config = carregarAuthConfig({
      KEYCLOAK_ISSUER_URI: 'http://keycloak:8080/realms/crudao',
    });

    expect(urlToken(config)).toBe(
      'http://keycloak:8080/realms/crudao/protocol/openid-connect/token',
    );
  });
});

describe('urlLogout', () => {
  it('inclui id_token_hint quando fornecido', () => {
    const config = carregarAuthConfig({
      KEYCLOAK_ISSUER_URI_PUBLIC: 'http://localhost:8081/realms/crudao',
      APP_BASE_URL: 'http://localhost:3000',
    });

    const url = new URL(urlLogout(config, 'meu-id-token'));

    expect(url.searchParams.get('id_token_hint')).toBe('meu-id-token');
    expect(url.searchParams.get('post_logout_redirect_uri')).toBe('http://localhost:3000');
  });

  it('omite id_token_hint quando não fornecido', () => {
    const config = carregarAuthConfig({
      KEYCLOAK_ISSUER_URI_PUBLIC: 'http://localhost:8081/realms/crudao',
    });

    const url = new URL(urlLogout(config, undefined));

    expect(url.searchParams.has('id_token_hint')).toBe(false);
  });
});
