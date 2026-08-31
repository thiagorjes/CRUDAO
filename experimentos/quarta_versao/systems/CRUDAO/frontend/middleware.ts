import { NextRequest, NextResponse } from "next/server";
import { renovarTokens } from "@/lib/oidc";
import { SESSION_COOKIE, cifrarSessao, cookieBaseOptions, decifrarSessao } from "@/lib/session";

/**
 * Guarda de rota (RF-014): sem sessão válida → redireciona ao login. Também é o único lugar que
 * pode renovar e *persistir* o cookie de sessão perto de expirar — Server Components não podem
 * gravar cookies (ver `lib/api.ts`).
 */
export async function middleware(req: NextRequest) {
  const bruto = req.cookies.get(SESSION_COOKIE)?.value;
  const sessao = bruto ? await decifrarSessao(bruto) : null;

  if (!sessao) {
    return NextResponse.redirect(new URL("/login", req.url));
  }

  const prestesAExpirar = Date.now() > sessao.expiresAt - 30_000;
  if (prestesAExpirar) {
    if (!sessao.refreshToken) {
      return limparSessaoERedirecionar(req);
    }
    const renovado = await renovarTokens(sessao.refreshToken);
    if (!renovado) {
      return limparSessaoERedirecionar(req);
    }

    // idToken não é persistido (achado de execução real, TASK-08.3 — cookie passava de 4KB, o
    // limite prático por cookie na maioria dos browsers, e era silenciosamente descartado; ver
    // login/oauth2/code/keycloak/route.ts).
    const novaSessaoCifrada = await cifrarSessao({
      accessToken: renovado.access_token,
      refreshToken: renovado.refresh_token ?? sessao.refreshToken,
      expiresAt: Date.now() + renovado.expires_in * 1000,
    });

    const res = NextResponse.next();
    res.cookies.set({
      name: SESSION_COOKIE,
      value: novaSessaoCifrada,
      ...cookieBaseOptions(),
      maxAge: 60 * 60 * 24 * 30,
    });
    return res;
  }

  return NextResponse.next();
}

function limparSessaoERedirecionar(req: NextRequest) {
  const res = NextResponse.redirect(new URL("/login", req.url));
  res.cookies.delete(SESSION_COOKIE);
  return res;
}

export const config = {
  matcher: [
    // Protege tudo exceto: next assets, login, auth endpoints
    "/((?!_next|favicon.ico|login|api/auth|sitemap.xml|robots.txt).*)",
  ],
};
