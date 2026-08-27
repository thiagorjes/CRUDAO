import { describe, expect, it, vi } from 'vitest';
import { carregarAuthConfig } from './config';
import { renovarSessao, trocarCodePorSessao } from './token-exchange';

const config = carregarAuthConfig({
  KEYCLOAK_ISSUER_URI: 'http://keycloak:8080/realms/crudao',
  KEYCLOAK_CLIENT_ID: 'crudao-app',
  KEYCLOAK_CLIENT_SECRET: 'segredo',
  APP_BASE_URL: 'http://localhost:3000',
});

describe('trocarCodePorSessao', () => {
  it('troca o code por uma sessão com expiração calculada a partir de expires_in', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: () =>
        Promise.resolve({
          access_token: 'token-de-acesso',
          refresh_token: 'token-de-refresh',
          id_token: 'token-de-id',
          expires_in: 300,
        }),
    });

    const antes = Date.now();
    const sessao = await trocarCodePorSessao(config, 'meu-code', fetchMock as unknown as typeof fetch);

    expect(sessao.accessToken).toBe('token-de-acesso');
    expect(sessao.refreshToken).toBe('token-de-refresh');
    expect(sessao.expiraEm).toBeGreaterThanOrEqual(antes + 300 * 1000);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('http://keycloak:8080/realms/crudao/protocol/openid-connect/token');
    const body = new URLSearchParams(init.body as string);
    expect(body.get('grant_type')).toBe('authorization_code');
    expect(body.get('code')).toBe('meu-code');
    expect(body.get('client_secret')).toBe('segredo');
  });

  it('lança erro quando o Keycloak responde com status de erro', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: false, status: 400 });

    await expect(
      trocarCodePorSessao(config, 'code-invalido', fetchMock as unknown as typeof fetch),
    ).rejects.toThrow('HTTP 400');
  });
});

describe('renovarSessao', () => {
  it('usa grant_type=refresh_token com o refresh_token informado', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: () =>
        Promise.resolve({
          access_token: 'novo-token',
          expires_in: 60,
        }),
    });

    const sessao = await renovarSessao(config, 'refresh-atual', fetchMock as unknown as typeof fetch);

    expect(sessao.accessToken).toBe('novo-token');
    const [, init] = fetchMock.mock.calls[0];
    const body = new URLSearchParams(init.body as string);
    expect(body.get('grant_type')).toBe('refresh_token');
    expect(body.get('refresh_token')).toBe('refresh-atual');
  });
});
