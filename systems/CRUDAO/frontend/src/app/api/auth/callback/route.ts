import { NextRequest, NextResponse } from 'next/server';
import { carregarAuthConfig } from '@/lib/auth/config';
import { COOKIE_STATE, gravarSessao } from '@/lib/auth/session';
import { trocarCodePorSessao } from '@/lib/auth/token-exchange';

/** Callback do Authorization Code flow (RF-014): troca `code` por tokens e abre a sessão. */
export async function GET(request: NextRequest): Promise<NextResponse> {
  const code = request.nextUrl.searchParams.get('code');
  const state = request.nextUrl.searchParams.get('state');
  const cookieState = request.cookies.get(COOKIE_STATE)?.value;

  if (!code || !state || !cookieState) {
    return NextResponse.redirect(new URL('/api/auth/login', request.url));
  }

  const { state: stateEsperado, returnTo } = JSON.parse(cookieState) as {
    state: string;
    returnTo: string;
  };
  if (state !== stateEsperado) {
    return NextResponse.redirect(new URL('/api/auth/login', request.url));
  }

  const config = carregarAuthConfig();
  const sessao = await trocarCodePorSessao(config, code);
  await gravarSessao(sessao);

  const resposta = NextResponse.redirect(new URL(returnTo || '/', request.url));
  resposta.cookies.delete(COOKIE_STATE);
  return resposta;
}
