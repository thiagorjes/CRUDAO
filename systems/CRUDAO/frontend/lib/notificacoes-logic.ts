import { NotificacaoResponse } from "./notificacoes";

/** Lógica pura de notificações (RF-005, TASK-07.7) — extraída para ser testável sem DOM. */

/** Mais recentes primeiro — o backend já ordena assim, reforçado aqui após merges via STOMP (decorativo). */
export function ordenarPorDataDesc(notificacoes: NotificacaoResponse[]): NotificacaoResponse[] {
  return [...notificacoes].sort(
    (a, b) => new Date(b.criadoEm).getTime() - new Date(a.criadoEm).getTime(),
  );
}

/** Insere/atualiza uma notificação recebida via STOMP na lista atual, sem duplicar por id. */
export function mesclarNotificacao(
  atuais: NotificacaoResponse[],
  recebida: NotificacaoResponse,
): NotificacaoResponse[] {
  const semDuplicata = atuais.filter((n) => n.id !== recebida.id);
  return ordenarPorDataDesc([recebida, ...semDuplicata]);
}
