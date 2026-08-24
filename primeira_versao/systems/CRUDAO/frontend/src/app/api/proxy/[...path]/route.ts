import { NextRequest, NextResponse } from 'next/server';
import { carregarAuthConfig } from '@/lib/auth/config';
import { montarUrlBackend } from '@/lib/auth/proxy-url';
import { garantirSessaoValida, SessaoInvalidaError } from '@/lib/auth/renovacao';
import { gravarSessao, lerSessao } from '@/lib/auth/session';
import { renovarSessao } from '@/lib/auth/token-exchange';

const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:8080';

/**
 * Proxy autenticado (RF-014): repassa a chamada ao backend anexando o `Authorization: Bearer` a
 * partir da sessão em cookie `httpOnly` — o token nunca é exposto ao JS do browser. Renova o
 * access_token via `refresh_token` quando perto de expirar; se o refresh falhar, responde 401 em
 * vez de deixar a exceção propagar como 500 (o client sabe que precisa logar de novo).
 */
async function handler(
  request: NextRequest,
  { params }: { params: Promise<{ path: string[] }> },
): Promise<NextResponse> {
  const sessao = await lerSessao();
  if (!sessao) {
    return NextResponse.json({ error: 'Sessão não encontrada' }, { status: 401 });
  }

  let sessaoValida;
  try {
    const config = carregarAuthConfig();
    sessaoValida = await garantirSessaoValida(sessao, {
      renovar: (refreshToken) => renovarSessao(config, refreshToken),
      gravar: gravarSessao,
    });
  } catch (erro) {
    if (erro instanceof SessaoInvalidaError) {
      return NextResponse.json({ error: 'Sessão expirada' }, { status: 401 });
    }
    throw erro;
  }

  const { path } = await params;
  const url = montarUrlBackend(BACKEND_URL, path, request.nextUrl.search.replace(/^\?/, ''));

  const corpo = ['GET', 'HEAD'].includes(request.method) ? undefined : await request.text();
  const resposta = await fetch(url, {
    method: request.method,
    headers: {
      'Content-Type': request.headers.get('content-type') ?? 'application/json',
      Authorization: `Bearer ${sessaoValida.accessToken}`,
    },
    body: corpo,
  });

  const texto = await resposta.text();
  return new NextResponse(texto || null, {
    status: resposta.status,
    headers: { 'Content-Type': resposta.headers.get('content-type') ?? 'application/json' },
  });
}

export {
  handler as GET,
  handler as POST,
  handler as PUT,
  handler as PATCH,
  handler as DELETE,
};
