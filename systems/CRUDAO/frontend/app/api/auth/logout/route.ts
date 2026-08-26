import { NextRequest, NextResponse } from "next/server";
import { env } from "@/lib/env";
import { SESSION_COOKIE, decifrarSessao } from "@/lib/session";

/**
 * POST /api/auth/logout — delega ao backend (`POST /api/auth/logout`) a revogação do access token
 * (RFC 7009) e o RP-Initiated Logout no Keycloak, best-effort igual ao backend; sempre limpa a
 * sessão local do Next.js mesmo se o backend estiver indisponível (ADR-006 não se aplica a logout).
 */
export async function POST(req: NextRequest) {
  const sessaoCookie = req.cookies.get(SESSION_COOKIE)?.value;
  const sessao = sessaoCookie ? await decifrarSessao(sessaoCookie) : null;

  if (sessao) {
    try {
      await fetch(`${env.backendUrl()}/api/auth/logout`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${sessao.accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ idTokenHint: sessao.idToken }),
      });
    } catch {
      // best-effort — sessão local é encerrada de qualquer forma.
    }
  }

  const res = NextResponse.redirect(new URL("/login", req.url));
  res.cookies.delete(SESSION_COOKIE);
  return res;
}
