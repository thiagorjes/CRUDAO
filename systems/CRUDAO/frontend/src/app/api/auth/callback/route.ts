import { NextRequest, NextResponse } from 'next/server';
import { carregarAuthConfig } from '@/lib/auth/config';
import { caminhoRelativoSeguro } from '@/lib/auth/return-to';
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

  const estadoSalvo = analisarCookieState(cookieState);
  if (!estadoSalvo || state !== estadoSalvo.state) {
    return NextResponse.redirect(new URL('/api/auth/login', request.url));
  }

  const config = carregarAuthConfig();
  const sessao = await trocarCodePorSessao(config, code);
  await gravarSessao(sessao);

  // Revalidado aqui também (defesa em profundidade) — o cookie já é gravado com um returnTo
  // validado em login/route.ts, mas nunca confiar apenas na origem de um valor gravado a partir
  // de input do usuário sem revalidar no ponto de uso.
  const returnTo = caminhoRelativoSeguro(estadoSalvo.returnTo);
  const resposta = NextResponse.redirect(new URL(returnTo, request.url));
  resposta.cookies.delete(COOKIE_STATE);
  return resposta;
}

function analisarCookieState(valor: string): { state: string; returnTo: string } | null {
  try {
    const dados = JSON.parse(valor) as { state?: unknown; returnTo?: unknown };
    if (typeof dados.state !== 'string') {
      return null;
    }
    return { state: dados.state, returnTo: typeof dados.returnTo === 'string' ? dados.returnTo : '/' };
  } catch {
    return null;
  }
}
