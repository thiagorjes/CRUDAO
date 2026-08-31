import { apiFetchJson } from "./api";
import type { MeResponse } from "./types";

export { iniciais } from "./format";
export type { MeResponse } from "./types";

/** Espelha o contrato de GET /api/me (docs/techspec/kanban-tarefas/contracts/auth.md). */
export async function obterMe(): Promise<MeResponse> {
  return apiFetchJson<MeResponse>("/api/me");
}
