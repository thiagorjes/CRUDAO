import { describe, expect, it, vi } from 'vitest';
import { garantirSessaoValida, SessaoInvalidaError } from './renovacao';
import { Sessao } from './session';

function sessaoValida(): Sessao {
  return { accessToken: 'atual', expiraEm: Date.now() + 60_000 };
}

function sessaoExpiradaComRefresh(refreshToken = 'refresh-1'): Sessao {
  return { accessToken: 'antigo', refreshToken, expiraEm: Date.now() - 1_000 };
}

describe('garantirSessaoValida', () => {
  it('retorna a própria sessão quando ainda não expirou, sem chamar renovar', async () => {
    const renovar = vi.fn();
    const gravar = vi.fn();

    const resultado = await garantirSessaoValida(sessaoValida(), { renovar, gravar });

    expect(resultado.accessToken).toBe('atual');
    expect(renovar).not.toHaveBeenCalled();
  });

  it('renova e grava a nova sessão quando expirada', async () => {
    const nova: Sessao = { accessToken: 'novo', expiraEm: Date.now() + 60_000 };
    const renovar = vi.fn().mockResolvedValue(nova);
    const gravar = vi.fn().mockResolvedValue(undefined);

    const resultado = await garantirSessaoValida(sessaoExpiradaComRefresh(), {
      renovar,
      gravar,
    });

    expect(resultado.accessToken).toBe('novo');
    expect(gravar).toHaveBeenCalledWith(nova);
  });

  it('lança SessaoInvalidaError quando expirada sem refresh_token', async () => {
    const renovar = vi.fn();
    const gravar = vi.fn();
    const sessao: Sessao = { accessToken: 'antigo', expiraEm: Date.now() - 1_000 };

    await expect(garantirSessaoValida(sessao, { renovar, gravar })).rejects.toThrow(
      SessaoInvalidaError,
    );
    expect(renovar).not.toHaveBeenCalled();
  });

  it('lança SessaoInvalidaError (não propaga o erro cru) quando o refresh falha', async () => {
    const renovar = vi.fn().mockRejectedValue(new Error('HTTP 400'));
    const gravar = vi.fn();

    await expect(
      garantirSessaoValida(sessaoExpiradaComRefresh('refresh-falho'), { renovar, gravar }),
    ).rejects.toThrow(SessaoInvalidaError);
    expect(gravar).not.toHaveBeenCalled();
  });

  it('deduplica chamadas concorrentes com o mesmo refresh_token', async () => {
    let resolverRenovar!: (s: Sessao) => void;
    const renovar = vi.fn(
      () =>
        new Promise<Sessao>((resolve) => {
          resolverRenovar = resolve;
        }),
    );
    const gravar = vi.fn().mockResolvedValue(undefined);
    const sessao = sessaoExpiradaComRefresh('refresh-concorrente');

    const chamada1 = garantirSessaoValida(sessao, { renovar, gravar });
    const chamada2 = garantirSessaoValida(sessao, { renovar, gravar });

    resolverRenovar({ accessToken: 'renovado-uma-vez', expiraEm: Date.now() + 60_000 });
    const [resultado1, resultado2] = await Promise.all([chamada1, chamada2]);

    expect(renovar).toHaveBeenCalledTimes(1);
    expect(resultado1.accessToken).toBe('renovado-uma-vez');
    expect(resultado2.accessToken).toBe('renovado-uma-vez');
  });
});
