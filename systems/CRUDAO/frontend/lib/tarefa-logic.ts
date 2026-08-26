import { EtapaResponse } from "./board";
import { HistoricoEtapaResponse } from "./tarefa";

/** Lógica pura do detalhe da tarefa (RF-003, RF-006, RF-017) — extraída para ser testável sem DOM. */

/** Lead-time por etapa formatado (RF-006) — segundos → "Xd Xh" ou "—" quando zero/etapa não visitada. */
export function formatarDuracao(segundos: number): string {
  if (segundos <= 0) return "—";
  const dias = Math.floor(segundos / 86400);
  const horas = Math.floor((segundos % 86400) / 3600);
  const minutos = Math.floor((segundos % 3600) / 60);
  if (dias > 0) return `${dias}d ${horas}h`;
  if (horas > 0) return `${horas}h ${minutos}min`;
  return `${minutos}min`;
}

/** Nome da etapa pelo id, com fallback para etapas removidas/desconhecidas. */
export function nomeEtapa(etapaPorId: Map<string, EtapaResponse>, etapaId: string): string {
  return etapaPorId.get(etapaId)?.nome ?? "Etapa desconhecida";
}

/** RF-017 — histórico exibido em ordem cronológica (mais recente primeiro). */
export function ordenarAuditoriaDesc<T extends { dataHora: string }>(itens: T[]): T[] {
  return [...itens].sort((a, b) => new Date(b.dataHora).getTime() - new Date(a.dataHora).getTime());
}

/** RF-006 — histórico de etapas em ordem cronológica de entrada (mais antiga primeiro). */
export function ordenarHistoricoEtapas(itens: HistoricoEtapaResponse[]): HistoricoEtapaResponse[] {
  return [...itens].sort((a, b) => new Date(a.entradaEm).getTime() - new Date(b.entradaEm).getTime());
}

/** Congelamento pós-início (RN-016, contrato PUT /api/tarefas/{id}): título/descrição travados. */
export function camposEstruturaisBloqueados(iniciada: boolean): boolean {
  return iniciada;
}
