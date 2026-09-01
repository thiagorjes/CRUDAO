/**
 * Client de API para board e operações de tarefas.
 * Proxies server-side via Route Handlers — nunca expõe token ao browser.
 */

import type { BoardResponse, ApiError, BoardTarefa } from "../types";

/** GET /api/projetos/{projetoId}/board — carrega estado completo do board */
export async function carregarBoard(projetoId: string): Promise<BoardResponse> {
  const res = await fetch(`/api/board/${projetoId}`, { method: "GET" });
  if (!res.ok) {
    const err = (await res.json()) as ApiError;
    throw new Error(err.message || `Erro ao carregar board: ${res.status}`);
  }
  return res.json() as Promise<BoardResponse>;
}

/** POST /api/tarefas/{id}/mover — move tarefa entre etapas */
export async function moverTarefa(
  tarefaId: string,
  etapaDestinoId: string
): Promise<void> {
  const res = await fetch(`/api/tarefas/${tarefaId}/mover`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ etapaDestinoId }),
  });
  if (!res.ok) {
    const err = (await res.json()) as ApiError;
    throw new Error(err.message || `Erro ao mover tarefa: ${res.status}`);
  }
}

/** POST /api/projetos/{projetoId}/tarefas — cria novo card */
export async function criarTarefa(
  projetoId: string,
  dados: {
    titulo: string;
    descricaoEscopo?: string;
    responsavelId?: string;
    raiaId?: string;
  }
): Promise<BoardTarefa> {
  const res = await fetch(`/api/board/${projetoId}/tarefas`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(dados),
  });
  if (!res.ok) {
    const err = (await res.json()) as ApiError;
    throw new Error(err.message || `Erro ao criar tarefa: ${res.status}`);
  }
  return res.json() as Promise<BoardTarefa>;
}

/**
 * DELETE /api/tarefas/{id}?projetoId=... — exclui card.
 * O backend exige `projetoId` (valida permissão + projeto ativo).
 */
export async function excluirTarefa(
  tarefaId: string,
  projetoId: string
): Promise<void> {
  const res = await fetch(
    `/api/tarefas/${tarefaId}?projetoId=${encodeURIComponent(projetoId)}`,
    { method: "DELETE" }
  );
  if (!res.ok) {
    const err = (await res.json()) as ApiError;
    throw new Error(err.message || `Erro ao excluir tarefa: ${res.status}`);
  }
}

/** POST /api/tarefas/{id}/impedimento?projetoId=... — marca impedimento */
export async function marcarImpedimento(
  tarefaId: string,
  projetoId: string
): Promise<void> {
  const res = await fetch(
    `/api/tarefas/${tarefaId}/impedimento?projetoId=${encodeURIComponent(projetoId)}`,
    { method: "POST" }
  );
  if (!res.ok) {
    const err = (await res.json()) as ApiError;
    throw new Error(err.message || `Erro ao marcar impedimento: ${res.status}`);
  }
}

/** DELETE /api/tarefas/{id}/impedimento?projetoId=... — desmarca impedimento */
export async function desmarcarImpedimento(
  tarefaId: string,
  projetoId: string
): Promise<void> {
  const res = await fetch(
    `/api/tarefas/${tarefaId}/impedimento?projetoId=${encodeURIComponent(projetoId)}`,
    { method: "DELETE" }
  );
  if (!res.ok) {
    const err = (await res.json()) as ApiError;
    throw new Error(
      err.message || `Erro ao desmarcar impedimento: ${res.status}`
    );
  }
}
