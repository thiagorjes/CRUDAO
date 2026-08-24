import { NextRequest, NextResponse } from 'next/server';
import { COOKIE_SESSAO } from '@/lib/auth/session';

/**
 * Protege todas as rotas de página, redirecionando para o login quando não há sessão (RF-014).
 * Rotas de auth, o proxy autenticado (`/api/proxy`), `/api/ws-token` e assets estáticos ficam
 * fora — essas rotas de API validam a sessão por si mesmas e devolvem 401 (não faz sentido
 * redirecionar uma chamada `fetch` de API). Convenção `proxy.ts` do Next 16 (renomeada de
 * `middleware.ts`).
 */
export function proxy(request: NextRequest): NextResponse {
  const temSessao = request.cookies.has(COOKIE_SESSAO);
  if (temSessao) {
    return NextResponse.next();
  }

  const loginUrl = new URL('/api/auth/login', request.url);
  loginUrl.searchParams.set('returnTo', request.nextUrl.pathname + request.nextUrl.search);
  return NextResponse.redirect(loginUrl);
}

export const config = {
  matcher: ['/((?!api/auth|api/proxy|api/ws-token|_next/static|_next/image|favicon.ico).*)'],
};
