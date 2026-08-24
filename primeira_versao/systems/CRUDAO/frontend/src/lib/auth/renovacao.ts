import { Sessao, sessaoExpirada } from './session';

/** Sinaliza que a sessão não pôde ser validada/renovada — o chamador deve tratar como 401. */
export class SessaoInvalidaError extends Error {}

export type DependenciasRenovacao = {
  renovar: (refreshToken: string) => Promise<Sessao>;
  gravar: (sessao: Sessao) => Promise<void>;
};

/**
 * Deduplica renovações concorrentes do mesmo `refresh_token` dentro do processo — sem isso,
 * chamadas paralelas ao proxy com sessão expirada disparam múltiplos `grant_type=refresh_token`
 * simultâneos; se o Keycloak rotacionar o refresh_token (padrão em várias configs), a segunda
 * chamada invalidaria o token já consumido pela primeira, derrubando a sessão sob carga (finding
 * 🟡 do code review da TASK-05.0). Mitigação de melhor esforço válida para uma única instância do
 * processo Next.js — não cobre múltiplas réplicas do frontend.
 */
const renovacoesEmAndamento = new Map<string, Promise<Sessao>>();

/**
 * Garante uma sessão com access_token válido, renovando via refresh_token quando necessário.
 * Lança {@link SessaoInvalidaError} em vez de deixar a exceção do Keycloak propagar — o proxy
 * deve responder 401 (não 500) quando o refresh falha, para o client saber que precisa logar de
 * novo (finding 🟡 do code review da TASK-05.0).
 */
export async function garantirSessaoValida(
  sessao: Sessao,
  deps: DependenciasRenovacao,
): Promise<Sessao> {
  if (!sessaoExpirada(sessao)) {
    return sessao;
  }
  if (!sessao.refreshToken) {
    throw new SessaoInvalidaError('Sessão expirada e sem refresh_token disponível.');
  }

  const chave = sessao.refreshToken;
  let promessa = renovacoesEmAndamento.get(chave);
  if (!promessa) {
    promessa = deps
      .renovar(sessao.refreshToken)
      .finally(() => renovacoesEmAndamento.delete(chave));
    renovacoesEmAndamento.set(chave, promessa);
  }

  try {
    const renovada = await promessa;
    await deps.gravar(renovada);
    return renovada;
  } catch (erro) {
    throw new SessaoInvalidaError('Falha ao renovar a sessão via refresh_token.', {
      cause: erro,
    });
  }
}
