/** Espelha docs/techspec/kanban-tarefas/contracts/tarefas.md — GET /api/projetos/{id}/board. */
export type EtapaResponse = {
  id: string;
  nome: string;
  ordem: number;
  etapaFinal: boolean;
  transicoesSaida: string[];
};

export type RaiaResponse = {
  id: string;
  nome: string;
  ordem: number;
  global: boolean;
};

export type TarefaBoardItemResponse = {
  id: string;
  titulo: string;
  etapaAtualId: string;
  raiaId: string;
  responsavelId: string | null;
  impedida: boolean;
  impedidaDesde: string | null;
  iniciada: boolean;
};

export type BoardResponse = {
  etapas: EtapaResponse[];
  raias: RaiaResponse[];
  tarefas: TarefaBoardItemResponse[];
};

/** Espelha docs/techspec/kanban-tarefas/contracts/papeis-permissoes.md — GET /api/projetos/{id}/usuarios. */
export type MembroProjeto = {
  usuarioId: string;
  nome: string;
  email: string;
  papeis: string[];
};
