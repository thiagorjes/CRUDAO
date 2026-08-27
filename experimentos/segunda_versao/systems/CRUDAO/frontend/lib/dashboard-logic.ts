import { DashboardEtapaResponse } from "./dashboard";

/** Lógica pura do dashboard (RF-007, TASK-07.6) — extraída para ser testável sem DOM. */

/** Formata segundos em uma duração legível (ex.: "2d 3h", "45min", "30s") — mesmo padrão de `lib/tarefa-logic.ts`. */
export function formatarDuracao(segundos: number): string {
  if (segundos <= 0) return "0s";

  const dias = Math.floor(segundos / 86400);
  const horas = Math.floor((segundos % 86400) / 3600);
  const minutos = Math.floor((segundos % 3600) / 60);
  const restoSegundos = Math.floor(segundos % 60);

  if (dias > 0) return horas > 0 ? `${dias}d ${horas}h` : `${dias}d`;
  if (horas > 0) return minutos > 0 ? `${horas}h ${minutos}min` : `${horas}h`;
  if (minutos > 0) return `${minutos}min`;
  return `${restoSegundos}s`;
}

/** Etapas ordenadas por lead-time médio decrescente — destaca os maiores gargalos primeiro. */
export function ordenarPorLeadTimeDesc(etapas: DashboardEtapaResponse[]): DashboardEtapaResponse[] {
  return [...etapas].sort((a, b) => b.leadTimeMedioSegundos - a.leadTimeMedioSegundos);
}
