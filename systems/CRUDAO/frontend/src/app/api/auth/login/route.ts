import { randomBytes } from 'crypto';
import { NextRequest, NextResponse } from 'next/server';
import { carregarAuthConfig, urlAutorizacao } from '@/lib/auth/config';
import { COOKIE_SECURE, COOKIE_STATE } from '@/lib/auth/session';

/** Inicia o Authorization Code flow (RF-014) redirecionando o browser ao Keycloak. */
export async function GET(request: NextRequest): Promise<NextResponse> {
  const config = carregarAuthConfig();
  const state = randomBytes(16).toString('hex');
  const returnTo = request.nextUrl.searchParams.get('returnTo') ?? '/';

  const resposta = NextResponse.redirect(urlAutorizacao(config, state));
  resposta.cookies.set(COOKIE_STATE, JSON.stringify({ state, returnTo }), {
    httpOnly: true,
    sameSite: 'lax',
    secure: COOKIE_SECURE,
    path: '/',
    maxAge: 300,
  });
  return resposta;
}
