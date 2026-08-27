/** Espelha docs/techspec/kanban-tarefas/contracts/tarefas.md — GET /api/tarefas/{id}. */
export type HistoricoEtapaResponse = {
  etapaId: string;
  entradaEm: string;
  saidaEm: string | null;
  leadTimeSegundos: number;
};

export type TarefaDetalheResponse = {
  id: string;
  titulo: string;
  descricaoEscopo: string | null;
  etapaAtualId: string;
  raiaId: string;
  responsavelId: string | null;
  iniciada: boolean;
  impedida: boolean;
  impedidaDesde: string | null;
  historicoEtapas: HistoricoEtapaResponse[];
  tempoImpedimentoTotalSegundos: number;
};

/** Espelha docs/techspec/kanban-tarefas/contracts/tarefas.md — GET /api/tarefas/{id}/auditoria. */
export type TarefaAuditoriaResponse = {
  autorId: string;
  campo: string;
  valorAnterior: string | null;
  valorNovo: string | null;
  dataHora: string;
};

/** Campos aceitos dependem de `iniciada` (contrato) — ver EditarTarefaRequest no backend. */
export type EditarTarefaRequest = {
  titulo?: string;
  descricaoEscopo?: string;
  responsavelId?: string;
  removerResponsavel: boolean;
};
