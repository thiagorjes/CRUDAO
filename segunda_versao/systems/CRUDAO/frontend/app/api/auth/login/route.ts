import { NextResponse } from "next/server";
import { montarUrlAutorizacao } from "@/lib/oidc";
import { gerarValorAleatorio, desafioPkce } from "@/lib/pkce";
import { OIDC_STATE_COOKIE, cookieBaseOptions } from "@/lib/session";

/** GET /api/auth/login — inicia o Authorization Code Flow (PKCE) direto com o Keycloak. */
export async function GET() {
  const state = gerarValorAleatorio(16);
  const nonce = gerarValorAleatorio(16);
  const codeVerifier = gerarValorAleatorio(32);
  const codeChallenge = await desafioPkce(codeVerifier);

  const url = montarUrlAutorizacao({ state, nonce, codeChallenge });

  const res = NextResponse.redirect(url);
  res.cookies.set({
    name: OIDC_STATE_COOKIE,
    value: JSON.stringify({ state, nonce, codeVerifier }),
    ...cookieBaseOptions(),
    maxAge: 300,
  });
  return res;
}
