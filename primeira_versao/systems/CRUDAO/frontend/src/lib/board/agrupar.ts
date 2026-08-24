import { Raia, Tarefa } from '@/lib/api/types';

export const RAIA_SEM_RAIA_ID = '__sem-raia__';

/**
 * Agrupa tarefas por (raiaId, etapaAtualId) para renderizar a grade do board. Tarefas cuja
 * `raiaId` não corresponde a nenhuma raia listada (raia excluída, ou tarefa sem raia atribuída)
 * caem no grupo `RAIA_SEM_RAIA_ID`, em vez de somem silenciosamente da visão do board.
 */
export function agruparPorRaiaEEtapa(
  tarefas: Tarefa[],
  raias: Raia[],
): Map<string, Map<string, Tarefa[]>> {
  const idsDeRaiaConhecidos = new Set(raias.map((r) => r.id));
  const grade = new Map<string, Map<string, Tarefa[]>>();

  for (const tarefa of tarefas) {
    const raiaId =
      tarefa.raiaId && idsDeRaiaConhecidos.has(tarefa.raiaId) ? tarefa.raiaId : RAIA_SEM_RAIA_ID;
    if (!grade.has(raiaId)) {
      grade.set(raiaId, new Map());
    }
    const porEtapa = grade.get(raiaId)!;
    if (!porEtapa.has(tarefa.etapaAtualId)) {
      porEtapa.set(tarefa.etapaAtualId, []);
    }
    porEtapa.get(tarefa.etapaAtualId)!.push(tarefa);
  }

  return grade;
}
