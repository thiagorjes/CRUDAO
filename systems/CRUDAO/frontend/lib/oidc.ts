import { createRemoteJWKSet, jwtVerify } from "jose";
import { env } from "./env";

/** Cliente OIDC próprio do Next.js (BFF) contra o Keycloak — decisão TASK-07.1 (ver memory/state.md). */

export function redirectUriCallback(): string {
  return `${env.appUrl()}/login/oauth2/code/keycloak`;
}

function endpointAutorizacao(): string {
  return `${env.keycloakIssuer()}/protocol/openid-connect/auth`;
}

function endpointToken(): string {
  return `${env.keycloakIssuer()}/protocol/openid-connect/token`;
}

export function endpointEncerrarSessao(): string {
  return `${env.keycloakIssuer()}/protocol/openid-connect/logout`;
}

function endpointJwks(): string {
  return `${env.keycloakIssuer()}/protocol/openid-connect/certs`;
}

let jwksCache: ReturnType<typeof createRemoteJWKSet> | null = null;
let jwksCacheIssuer: string | null = null;

function jwks() {
  const issuer = env.keycloakIssuer();
  if (!jwksCache || jwksCacheIssuer !== issuer) {
    jwksCache = createRemoteJWKSet(new URL(endpointJwks()));
    jwksCacheIssuer = issuer;
  }
  return jwksCache;
}

export function montarUrlAutorizacao(params: {
  state: string;
  nonce: string;
  codeChallenge: string;
}): string {
  const url = new URL(endpointAutorizacao());
  url.searchParams.set("client_id", env.keycloakClientId());
  url.searchParams.set("response_type", "code");
  url.searchParams.set("scope", "openid profile email");
  url.searchParams.set("redirect_uri", redirectUriCallback());
  url.searchParams.set("state", params.state);
  url.searchParams.set("nonce", params.nonce);
  url.searchParams.set("code_challenge", params.codeChallenge);
  url.searchParams.set("code_challenge_method", "S256");
  return url.toString();
}

type TokenResponse = {
  access_token: string;
  refresh_token?: string;
  id_token?: string;
  expires_in: number;
};

export async function trocarCodigoPorTokens(params: {
  code: string;
  codeVerifier: string;
}): Promise<TokenResponse> {
  const body = new URLSearchParams();
  body.set("grant_type", "authorization_code");
  body.set("code", params.code);
  body.set("redirect_uri", redirectUriCallback());
  body.set("client_id", env.keycloakClientId());
  body.set("client_secret", env.keycloakClientSecret());
  body.set("code_verifier", params.codeVerifier);

  const res = await fetch(endpointToken(), {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body,
  });
  if (!res.ok) {
    throw new Error(`Falha na troca do código de autorização (status ${res.status})`);
  }
  return res.json();
}

/**
 * Verifica assinatura (JWKS do Keycloak), `iss`, `aud` e o `nonce` do `id_token` contra o valor
 * gerado no início do login — sem isso, o `nonce` propagado pelo fluxo era gerado mas nunca
 * checado (achado de code review, TASK-07.1). Lança em qualquer falha; chamador trata como login
 * inválido.
 */
export async function verificarIdToken(idToken: string, nonceEsperado: string): Promise<void> {
  const { payload } = await jwtVerify(idToken, jwks(), {
    issuer: env.keycloakIssuer(),
    audience: env.keycloakClientId(),
  });
  if (payload.nonce !== nonceEsperado) {
    throw new Error("nonce do id_token não confere");
  }
}

/** Retorna `null` em qualquer falha — refresh_token inválido/expirado força novo login. */
export async function renovarTokens(refreshToken: string): Promise<TokenResponse | null> {
  const body = new URLSearchParams();
  body.set("grant_type", "refresh_token");
  body.set("refresh_token", refreshToken);
  body.set("client_id", env.keycloakClientId());
  body.set("client_secret", env.keycloakClientSecret());

  try {
    const res = await fetch(endpointToken(), {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body,
    });
    if (!res.ok) return null;
    return res.json();
  } catch {
    return null;
  }
}
