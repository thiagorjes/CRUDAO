import { EtapaResponse, TarefaBoardItemResponse } from "./board";

/** Lógica pura do board (RF-002, RF-004, RF-018, RF-019) — extraída para ser testável sem DOM. */

export function tarefasDe(
  tarefas: TarefaBoardItemResponse[],
  raiaId: string,
  etapaId: string,
): TarefaBoardItemResponse[] {
  return tarefas.filter((t) => t.raiaId === raiaId && t.etapaAtualId === etapaId);
}

/** RF-002/RN-003 — transição só é válida se configurada em `EtapaResponse.transicoesSaida`. */
export function transicaoPermitida(
  etapaPorId: Map<string, EtapaResponse>,
  origemId: string,
  destinoId: string,
): boolean {
  if (origemId === destinoId) return false;
  return etapaPorId.get(origemId)?.transicoesSaida.includes(destinoId) ?? false;
}

export function etapaDeMenorOrdem(etapas: EtapaResponse[]): EtapaResponse | undefined {
  return [...etapas].sort((a, b) => a.ordem - b.ordem)[0];
}

export function classeDestaque(
  arrastando: { etapaOrigemId: string } | null,
  etapaId: string,
  etapaPorId: Map<string, EtapaResponse>,
): string {
  if (!arrastando || arrastando.etapaOrigemId === etapaId) return "";
  return transicaoPermitida(etapaPorId, arrastando.etapaOrigemId, etapaId)
    ? "column--drop-valid"
    : "column--drop-invalid";
}

/** Mensagens de erro claras por status HTTP (critério de aceite TASK-07.2: "erro claro"). */
export function mensagemErro(status: number, padrao: string): string {
  if (status === 409) {
    return `${padrao} A transição não está configurada para esta etapa.`;
  }
  if (status === 403) {
    return `${padrao} Você não tem permissão para esta ação.`;
  }
  return padrao;
}
