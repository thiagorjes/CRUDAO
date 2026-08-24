/**
 * Configuração OIDC (Keycloak) — RF-014, ADR-003.
 *
 * Duas bases de URL são necessárias: `issuerUriServer` é usada para chamadas feitas pelo
 * processo Next.js (troca de `code` por tokens, refresh) e precisa ser alcançável de dentro da
 * rede onde o frontend roda (ex.: `http://keycloak:8080` dentro do docker-compose);
 * `issuerUriPublic` é usada para montar URLs para as quais o *browser* é redirecionado
 * (`/authorize`, `/logout`) e precisa ser alcançável do host (ex.: `http://localhost:8081`).
 * Mesma questão enfrentada na validação REST da TASK-03.1 (mismatch de `iss` no JWT).
 */
export type AuthConfig = {
  issuerUriServer: string;
  issuerUriPublic: string;
  clientId: string;
  clientSecret: string;
  appBaseUrl: string;
};

export function carregarAuthConfig(
  env: Partial<NodeJS.ProcessEnv> = process.env,
): AuthConfig {
  const issuerUriServer = env.KEYCLOAK_ISSUER_URI ?? 'http://localhost:8081/realms/crudao';
  return {
    issuerUriServer,
    issuerUriPublic: env.KEYCLOAK_ISSUER_URI_PUBLIC ?? issuerUriServer,
    clientId: env.KEYCLOAK_CLIENT_ID ?? 'crudao-app',
    clientSecret: env.KEYCLOAK_CLIENT_SECRET ?? 'crudao-app-secret-dev',
    appBaseUrl: env.APP_BASE_URL ?? 'http://localhost:3000',
  };
}

export function urlAutorizacao(config: AuthConfig, state: string): string {
  const params = new URLSearchParams({
    client_id: config.clientId,
    redirect_uri: `${config.appBaseUrl}/api/auth/callback`,
    response_type: 'code',
    scope: 'openid profile email',
    state,
  });
  return `${config.issuerUriPublic}/protocol/openid-connect/auth?${params.toString()}`;
}

export function urlToken(config: AuthConfig): string {
  return `${config.issuerUriServer}/protocol/openid-connect/token`;
}

export function urlLogout(config: AuthConfig, idTokenHint: string | undefined): string {
  const params = new URLSearchParams({
    post_logout_redirect_uri: config.appBaseUrl,
  });
  if (idTokenHint) {
    params.set('id_token_hint', idTokenHint);
  }
  return `${config.issuerUriPublic}/protocol/openid-connect/logout?${params.toString()}`;
}
