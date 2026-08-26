import { apiFetchJson } from "./api";

export { iniciais } from "./format";

/** Espelha o contrato de GET /api/me (docs/techspec/kanban-tarefas/contracts/auth.md). */
export type MeResponse = {
  id: string;
  nome: string;
  email: string;
  adminGlobal: boolean;
  projetos: { projetoId: string; papeis: string[] }[];
};

export async function obterMe(): Promise<MeResponse> {
  return apiFetchJson<MeResponse>("/api/me");
}
