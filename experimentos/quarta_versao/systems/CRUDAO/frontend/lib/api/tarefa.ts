import type { TarefaDetalhe, AuditoriaEntry, EditarTarefaRequest, ApiError } from "@/lib/types";

/**
 * Obter detalhe completo de uma tarefa
 * GET /api/tarefas/{tarefaId}
 */
export async function obterTarefaDetalhe(tarefaId: string): Promise<TarefaDetalhe> {
  const res = await fetch(`/api/tarefas/${tarefaId}`, {
    method: "GET",
    credentials: "include",
  });

  if (!res.ok) {
    const error = (await res.json()) as ApiError;
    throw new Error(error.message || "Erro ao obter tarefa");
  }

  return res.json();
}

/**
 * Obter histórico de auditoria de uma tarefa
 * GET /api/tarefas/{tarefaId}/auditoria
 */
export async function obterAuditoria(tarefaId: string): Promise<AuditoriaEntry[]> {
  const res = await fetch(`/api/tarefas/${tarefaId}/auditoria`, {
    method: "GET",
    credentials: "include",
  });

  if (!res.ok) {
    const error = (await res.json()) as ApiError;
    throw new Error(error.message || "Erro ao obter auditoria");
  }

  return res.json();
}

/**
 * Editar tarefa (titulo, descricao, responsavel).
 * PUT /api/tarefas/{tarefaId} — backend responde 204 No Content; recarregue com
 * `obterTarefaDetalhe` após chamar esta função.
 */
export async function editarTarefa(tarefaId: string, dados: EditarTarefaRequest): Promise<void> {
  const res = await fetch(`/api/tarefas/${tarefaId}`, {
    method: "PUT",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(dados),
  });

  if (!res.ok) {
    const error = (await res.json()) as ApiError;
    throw new Error(error.message || "Erro ao editar tarefa");
  }
}

/**
 * Adicionar observador à tarefa
 * POST /api/tarefas/{tarefaId}/observadores
 */
export async function adicionarObservador(
  tarefaId: string,
  usuarioId: string
): Promise<void> {
  const res = await fetch(`/api/tarefas/${tarefaId}/observadores`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ usuarioId }),
  });

  if (!res.ok) {
    const error = (await res.json()) as ApiError;
    throw new Error(error.message || "Erro ao adicionar observador");
  }
}

/**
 * Remover observador da tarefa
 * DELETE /api/tarefas/{tarefaId}/observadores/{usuarioId}
 */
export async function removerObservador(
  tarefaId: string,
  usuarioId: string
): Promise<void> {
  const res = await fetch(`/api/tarefas/${tarefaId}/observadores/${usuarioId}`, {
    method: "DELETE",
    credentials: "include",
  });

  if (!res.ok) {
    const error = (await res.json()) as ApiError;
    throw new Error(error.message || "Erro ao remover observador");
  }
}

/**
 * Obter lista de usuários associados ao projeto (dedupe por usuário — um usuário pode ter mais
 * de um papel). GET /api/projetos/{projetoId}/usuarios → UsuarioProjetoResponse[] (backend).
 */
export async function obterUsuariosProjeto(projetoId: string): Promise<{ id: string; nome: string }[]> {
  const res = await fetch(`/api/projetos/${projetoId}/usuarios`, {
    method: "GET",
    credentials: "include",
  });

  if (!res.ok) {
    const error = (await res.json()) as ApiError;
    throw new Error(error.message || "Erro ao obter usuários do projeto");
  }

  const vinculos = (await res.json()) as { usuarioId: string; usuarioNome: string }[];
  const vistos = new Set<string>();
  return vinculos
    .filter((v) => (vistos.has(v.usuarioId) ? false : (vistos.add(v.usuarioId), true)))
    .map((v) => ({ id: v.usuarioId, nome: v.usuarioNome }));
}
