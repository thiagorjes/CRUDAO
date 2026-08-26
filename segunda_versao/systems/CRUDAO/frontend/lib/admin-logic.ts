import { EtapaResponse } from "./board";

/**
 * Lógica pura do admin de projeto (RF-008/009/010/011, TASK-07.4) — extraída para ser testável
 * sem DOM, mesmo padrão de `board-logic.ts`.
 */

/**
 * Espelho client-side de RN-003 (decorativo — backend sempre revalida em `PUT /api/etapas/{id}/transicoes`):
 * etapa não-final precisa de ao menos uma transição de saída para ser considerada operacional.
 */
export function etapaSemTransicaoObrigatoria(etapa: Pick<EtapaResponse, "etapaFinal" | "transicoesSaida">): boolean {
  return !etapa.etapaFinal && etapa.transicoesSaida.length === 0;
}

export function nomesTransicoesSaida(
  etapa: Pick<EtapaResponse, "etapaFinal" | "transicoesSaida">,
  etapaPorId: Map<string, EtapaResponse>,
): string {
  if (etapa.transicoesSaida.length === 0) {
    return etapa.etapaFinal ? "— (permite desfinalizar)" : "—";
  }
  return etapa.transicoesSaida.map((id) => etapaPorId.get(id)?.nome ?? id).join(", ");
}

export function ordenarPorOrdem<T extends { ordem: number }>(itens: T[]): T[] {
  return [...itens].sort((a, b) => a.ordem - b.ordem);
}

/** Mensagens de erro claras por status HTTP, específicas do admin (critério de aceite da task). */
export function mensagemErroAdmin(status: number, padrao: string): string {
  if (status === 409) {
    return `${padrao} Há tarefas ativas vinculadas a este recurso.`;
  }
  if (status === 422) {
    return `${padrao} Verifique os dados informados (ex.: etapa não-final precisa de transição de saída).`;
  }
  if (status === 403) {
    return `${padrao} Você não tem permissão para esta ação, ou o projeto está finalizado.`;
  }
  return padrao;
}
