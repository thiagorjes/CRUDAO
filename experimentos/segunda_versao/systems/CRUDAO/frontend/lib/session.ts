import { EncryptJWT, jwtDecrypt } from "jose";
import { env } from "./env";

/** Cookie httpOnly que carrega a sessão OIDC cifrada — o token nunca chega ao JS do browser. */
export const SESSION_COOKIE = "kanban_session";

/** Cookie httpOnly de curta duração usado só durante o round-trip do Authorization Code Flow. */
export const OIDC_STATE_COOKIE = "kanban_oidc_state";

export type SessionData = {
  accessToken: string;
  refreshToken?: string;
  idToken?: string;
  /** epoch millis */
  expiresAt: number;
};

async function chaveDerivada(): Promise<Uint8Array> {
  const dados = new TextEncoder().encode(env.sessionSecret());
  const digest = await crypto.subtle.digest("SHA-256", dados);
  return new Uint8Array(digest);
}

export async function cifrarSessao(dados: SessionData): Promise<string> {
  const chave = await chaveDerivada();
  return new EncryptJWT({ ...dados })
    .setProtectedHeader({ alg: "dir", enc: "A256GCM" })
    .setIssuedAt()
    .setExpirationTime("30d")
    .encrypt(chave);
}

export async function decifrarSessao(token: string): Promise<SessionData | null> {
  try {
    const chave = await chaveDerivada();
    const { payload } = await jwtDecrypt(token, chave);
    return payload as unknown as SessionData;
  } catch {
    return null;
  }
}

export function cookieBaseOptions() {
  return {
    httpOnly: true as const,
    secure: env.appUrl().startsWith("https"),
    sameSite: "lax" as const,
    path: "/",
  };
}
