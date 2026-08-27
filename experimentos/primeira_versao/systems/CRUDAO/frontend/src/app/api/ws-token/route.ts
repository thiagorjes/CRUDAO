import { NextResponse } from 'next/server';
import { lerSessao } from '@/lib/auth/session';

/**
 * Expõe o `access_token` atual para o header CONNECT do cliente STOMP (RF-005) — achado de code
 * review da TASK-05.1: sem isso, o handshake WebSocket ficava sem autenticação (qualquer cliente
 * podia se inscrever no tópico de board de qualquer projeto).
 *
 * <p>Exceção deliberada e documentada à regra geral de "token nunca chega ao JS do browser"
 * (TASK-05.0): o STOMP CONNECT roda no client (a lib @stomp/stompjs conecta direto do browser,
 * não passa pelo proxy Next.js — é uma conexão WebSocket de longa duração), então não há como
 * anexar o header sem o JS ter o token em mãos. O token não é persistido pelo client (não vai
 * para localStorage/cookie) — fica em memória só para o CONNECT.
 */
export async function GET(): Promise<NextResponse> {
  const sessao = await lerSessao();
  if (!sessao) {
    return NextResponse.json({ error: 'Sessão não encontrada' }, { status: 401 });
  }
  return NextResponse.json({ accessToken: sessao.accessToken });
}
