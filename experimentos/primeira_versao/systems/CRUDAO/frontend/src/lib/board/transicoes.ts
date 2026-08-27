import { Etapa, Transicao } from '@/lib/api/types';

export type AcaoTransicao = {
  transicaoId: string;
  etapaDestinoId: string;
  etapaDestinoNome: string;
  /** REABERTURA a partir da etapa final (RF-012) vs. NORMAL para etapa de ordem menor/maior. */
  rotulo: 'avancar' | 'retroceder' | 'reabertura';
};

/** Transições de saída da etapa informada (RF-002). */
export function transicoesDeOrigem(etapaId: string, transicoes: Transicao[]): Transicao[] {
  return transicoes.filter((t) => t.etapaOrigemId === etapaId);
}

/**
 * Ids de etapa que são destino válido a partir da etapa atual — usado para destacar as colunas
 * durante o drag (DDR-002) e para validar o drop antes de chamar a API.
 */
export function etapasAlvoValidas(etapaAtualId: string, transicoes: Transicao[]): Set<string> {
  return new Set(transicoesDeOrigem(etapaAtualId, transicoes).map((t) => t.etapaDestinoId));
}

export function transicaoPermitida(
  etapaOrigemId: string,
  etapaDestinoId: string,
  transicoes: Transicao[],
): boolean {
  return etapasAlvoValidas(etapaOrigemId, transicoes).has(etapaDestinoId);
}

/**
 * Ações disponíveis no menu do card (RF-002, RF-012, DDR-002) — uma por transição de saída da
 * etapa atual, rotulada por comparação de `ordem` (avançar/retroceder) ou por `tipo` REABERTURA
 * (desfinalizar). Workflows com múltiplas transições de saída na mesma direção geram múltiplas
 * ações — não assume uma cadeia linear como o protótipo de demonstração.
 */
export function acoesDoMenu(
  etapaAtualId: string,
  transicoes: Transicao[],
  etapas: Etapa[],
): AcaoTransicao[] {
  const etapaAtual = etapas.find((e) => e.id === etapaAtualId);
  const porId = new Map(etapas.map((e) => [e.id, e]));

  return transicoesDeOrigem(etapaAtualId, transicoes)
    .map((t) => {
      const destino = porId.get(t.etapaDestinoId);
      if (!destino) {
        return null;
      }
      const rotulo: AcaoTransicao['rotulo'] =
        t.tipo === 'REABERTURA'
          ? 'reabertura'
          : etapaAtual && destino.ordem < etapaAtual.ordem
            ? 'retroceder'
            : 'avancar';
      return {
        transicaoId: t.id,
        etapaDestinoId: destino.id,
        etapaDestinoNome: destino.nome,
        rotulo,
      };
    })
    .filter((a): a is AcaoTransicao => a !== null);
}
