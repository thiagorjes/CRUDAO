import { NextRequest, NextResponse } from "next/server";
import { trocarCodigoPorTokens, verificarIdToken } from "@/lib/oidc";
import { OIDC_STATE_COOKIE, SESSION_COOKIE, cifrarSessao, cookieBaseOptions } from "@/lib/session";

type EstadoOidc = { state: string; nonce: string; codeVerifier: string };

function estadoOidcValido(valor: unknown): valor is EstadoOidc {
  return (
    typeof valor === "object" &&
    valor !== null &&
    typeof (valor as EstadoOidc).state === "string" &&
    typeof (valor as EstadoOidc).nonce === "string" &&
    typeof (valor as EstadoOidc).codeVerifier === "string"
  );
}

/**
 * GET /login/oauth2/code/keycloak — callback do Authorization Code Flow.
 *
 * Caminho e client (`kanban-frontend`) casam exatamente com o `redirectUris` registrado no realm
 * dev (`systems/CRUDAO/keycloak/realm-export.json`) — não confundir com o `oauth2Login` do backend
 * Spring, que fica sem uso real neste fluxo (decisão TASK-07.1, ver memory/state.md).
 */
export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const code = searchParams.get("code");
  const state = searchParams.get("state");
  const erroOidc = searchParams.get("error");

  const estadoCookie = req.cookies.get(OIDC_STATE_COOKIE)?.value;

  if (erroOidc || !code || !state || !estadoCookie) {
    return redirecionarComErro(req);
  }

  let estadoSalvo: EstadoOidc;
  try {
    const parseado: unknown = JSON.parse(estadoCookie);
    if (!estadoOidcValido(parseado)) {
      return redirecionarComErro(req);
    }
    estadoSalvo = parseado;
  } catch {
    return redirecionarComErro(req);
  }

  // Proteção CSRF do fluxo OIDC — state devolvido pelo Keycloak deve bater com o gerado no início.
  if (estadoSalvo.state !== state) {
    return redirecionarComErro(req);
  }

  let tokens;
  try {
    tokens = await trocarCodigoPorTokens({ code, codeVerifier: estadoSalvo.codeVerifier });
  } catch {
    return redirecionarComErro(req);
  }

  // Assinatura (JWKS), iss/aud e nonce do id_token — sem isso o nonce era gerado mas nunca checado
  // (achado de code review, TASK-07.1).
  if (tokens.id_token) {
    try {
      await verificarIdToken(tokens.id_token, estadoSalvo.nonce);
    } catch {
      return redirecionarComErro(req);
    }
  }

  const sessaoCifrada = await cifrarSessao({
    accessToken: tokens.access_token,
    refreshToken: tokens.refresh_token,
    idToken: tokens.id_token,
    expiresAt: Date.now() + tokens.expires_in * 1000,
  });

  const res = NextResponse.redirect(new URL("/projetos", req.url));
  res.cookies.set({
    name: SESSION_COOKIE,
    value: sessaoCifrada,
    ...cookieBaseOptions(),
    maxAge: 60 * 60 * 24 * 30,
  });
  res.cookies.delete(OIDC_STATE_COOKIE);
  return res;
}

function redirecionarComErro(req: NextRequest) {
  const res = NextResponse.redirect(new URL("/login?erro=1", req.url));
  res.cookies.delete(OIDC_STATE_COOKIE);
  return res;
}
