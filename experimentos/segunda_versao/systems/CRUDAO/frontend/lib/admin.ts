import { EtapaResponse, RaiaResponse } from "./board";

export type { EtapaResponse, RaiaResponse };

/** Espelha docs/techspec/kanban-tarefas/contracts/projetos.md. */
export type ProjetoResponse = {
  id: string;
  nome: string;
  descricao: string | null;
  status: "ATIVO" | "FINALIZADO";
};

/** Espelha docs/techspec/kanban-tarefas/contracts/workflows.md — GET /api/projetos/{id}/workflows. */
export type WorkflowResponse = {
  id: string;
  nome: string;
  etapas: EtapaResponse[];
};
