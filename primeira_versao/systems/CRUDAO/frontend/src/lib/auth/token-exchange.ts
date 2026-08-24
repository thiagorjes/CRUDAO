import { AuthConfig, urlToken } from './config';
import { calcularExpiracao, Sessao } from './session';

type RespostaToken = {
  access_token: string;
  refresh_token?: string;
  id_token?: string;
  expires_in: number;
};

/** Troca o `code` de autorização pelos tokens (Authorization Code flow, RF-014). */
export async function trocarCodePorSessao(
  config: AuthConfig,
  code: string,
  fetchImpl: typeof fetch = fetch,
): Promise<Sessao> {
  const params = new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: config.clientId,
    client_secret: config.clientSecret,
    code,
    redirect_uri: `${config.appBaseUrl}/api/auth/callback`,
  });
  return trocarPorSessao(config, params, fetchImpl);
}

/** Renova a sessão a partir do `refresh_token` (RF-014). */
export async function renovarSessao(
  config: AuthConfig,
  refreshToken: string,
  fetchImpl: typeof fetch = fetch,
): Promise<Sessao> {
  const params = new URLSearchParams({
    grant_type: 'refresh_token',
    client_id: config.clientId,
    client_secret: config.clientSecret,
    refresh_token: refreshToken,
  });
  return trocarPorSessao(config, params, fetchImpl);
}

async function trocarPorSessao(
  config: AuthConfig,
  params: URLSearchParams,
  fetchImpl: typeof fetch,
): Promise<Sessao> {
  const resposta = await fetchImpl(urlToken(config), {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: params.toString(),
  });
  if (!resposta.ok) {
    throw new Error(`Falha ao obter token do Keycloak: HTTP ${resposta.status}`);
  }
  const dados = (await resposta.json()) as RespostaToken;
  return {
    accessToken: dados.access_token,
    refreshToken: dados.refresh_token,
    idToken: dados.id_token,
    expiraEm: calcularExpiracao(dados.expires_in),
  };
}
