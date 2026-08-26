/** Espelha docs/techspec/kanban-tarefas/contracts/dashboard-notificacoes.md — GET /api/notificacoes. */
export type NotificacaoResponse = {
  id: string;
  tarefaId: string;
  tipo: string;
  mensagem: string;
  lida: boolean;
  criadoEm: string;
};
