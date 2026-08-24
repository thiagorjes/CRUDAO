import { NextResponse } from 'next/server';
import { carregarAuthConfig, urlLogout } from '@/lib/auth/config';
import { lerSessao, limparSessao } from '@/lib/auth/session';

/** Encerra a sessão local e no Keycloak (RF-014). */
export async function GET(): Promise<NextResponse> {
  const config = carregarAuthConfig();
  const sessao = await lerSessao();
  await limparSessao();
  return NextResponse.redirect(urlLogout(config, sessao?.idToken));
}
