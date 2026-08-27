import { EventoBoard, Tarefa } from '@/lib/api/types';

type EstadoTarefas = { projetoId: string; tarefas: Tarefa[] };

/**
 * Aplica o evento `TAREFA_EXCLUIDA` (RF-002) ao estado local do board — extraído de
 * `BoardApp.atualizarTarefaLocal` para ser testável isoladamente (Vitest, sem React).
 * Mesmo guard de projeto usado pelos demais tipos de evento; no-op seguro se a tarefa já não
 * estiver mais no array (evento chegando depois da remoção otimista da própria ação).
 */
export function aplicarTarefaExcluida<T extends EstadoTarefas>(atual: T | null, evento: EventoBoard): T | null {
  if (!atual || atual.projetoId !== evento.projetoId) return atual;
  return { ...atual, tarefas: atual.tarefas.filter((t) => t.id !== evento.tarefaId) };
}
