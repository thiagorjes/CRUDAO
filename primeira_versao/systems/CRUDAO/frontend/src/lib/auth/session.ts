import { cookies } from 'next/headers';

/** Sessão local, persistida em cookie `httpOnly` — os tokens nunca chegam ao JS do browser. */
export type Sessao = {
  accessToken: string;
  refreshToken?: string;
  idToken?: string;
  /** epoch ms em que o access_token expira. */
  expiraEm: number;
};

export const COOKIE_SESSAO = 'crudao_session';
export const COOKIE_STATE = 'crudao_auth_state';

/**
 * `NODE_ENV=production` (setado no Dockerfile) não implica HTTPS — o deploy atual é on-premise
 * via Docker, sem terminação TLS nesta fase (architecture.md). Gatear `Secure` por `NODE_ENV`
 * faria o browser descartar os cookies de sessão ao servir por HTTP puro. Controlado por env var
 * explícita, a ser ligada quando um TLS termination for introduzido.
 */
export const COOKIE_SECURE = process.env.COOKIE_SECURE === 'true';

// Guardrail (G-AUTH-03, code review da TASK-05.0): avisa alto se o processo está marcado como
// produção sem `COOKIE_SECURE=true` — sinal de que o TLS termination foi introduzido (ou é
// esperado) mas a env var ficou esquecida, deixando o cookie de sessão sem `Secure`. Não bloqueia
// o boot (o deploy atual on-premise legitimamente roda sem TLS nesta fase), só torna o descuido
// visível nos logs em vez de silencioso.
if (process.env.NODE_ENV === 'production' && !COOKIE_SECURE) {
  console.warn(
    '[auth] NODE_ENV=production sem COOKIE_SECURE=true — cookies de sessão sairão sem o' +
      ' atributo Secure. Se este deploy já serve por HTTPS, defina COOKIE_SECURE=true.',
  );
}

export function calcularExpiracao(expiresInSegundos: number): number {
  return Date.now() + expiresInSegundos * 1000;
}

export function sessaoExpirada(sessao: Sessao, margemMs = 5_000): boolean {
  return Date.now() >= sessao.expiraEm - margemMs;
}

export async function lerSessao(): Promise<Sessao | null> {
  const cookieStore = await cookies();
  const valor = cookieStore.get(COOKIE_SESSAO)?.value;
  if (!valor) {
    return null;
  }
  try {
    return JSON.parse(valor) as Sessao;
  } catch {
    return null;
  }
}

export async function gravarSessao(sessao: Sessao): Promise<void> {
  const cookieStore = await cookies();
  cookieStore.set(COOKIE_SESSAO, JSON.stringify(sessao), {
    httpOnly: true,
    sameSite: 'lax',
    secure: COOKIE_SECURE,
    path: '/',
    maxAge: 60 * 60 * 24, // teto de 1 dia — refresh_token cobre a renovação real do access_token
  });
}

export async function limparSessao(): Promise<void> {
  const cookieStore = await cookies();
  cookieStore.delete(COOKIE_SESSAO);
}
