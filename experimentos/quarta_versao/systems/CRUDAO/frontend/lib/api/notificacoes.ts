import type { Notificacao, ApiError } from "@/lib/types";

/**
 * Lista as notificações não lidas do usuário autenticado.
 * GET /api/notificacoes
 */
export async function listarNaoLidas(): Promise<Notificacao[]> {
  const res = await fetch("/api/notificacoes", {
    method: "GET",
    credentials: "include",
  });

  if (!res.ok) {
    const error = (await res.json()) as ApiError;
    throw new Error(error.message || error.error || "Erro ao obter notificações");
  }

  return res.json();
}

/**
 * Marca uma notificação como lida.
 * PUT /api/notificacoes/{id}/marcar-como-lida
 */
export async function marcarComoLida(id: string): Promise<void> {
  const res = await fetch(`/api/notificacoes/${id}/marcar-como-lida`, {
    method: "PUT",
    credentials: "include",
  });

  if (!res.ok) {
    const error = (await res.json()) as ApiError;
    throw new Error(error.message || error.error || "Erro ao marcar notificação como lida");
  }
}
