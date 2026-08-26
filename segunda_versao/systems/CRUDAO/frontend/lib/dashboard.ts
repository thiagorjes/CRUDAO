/** Espelha docs/techspec/kanban-tarefas/contracts/dashboard-notificacoes.md — GET /api/projetos/{id}/dashboard. */
export type DashboardEtapaResponse = {
  etapaId: string;
  etapaNome: string;
  leadTimeMedioSegundos: number;
  tempoImpedimentoMedioSegundos: number;
};

export type DashboardResponse = {
  leadTimeMedioPorEtapa: DashboardEtapaResponse[];
  totalTarefasConsideradas: number;
};
