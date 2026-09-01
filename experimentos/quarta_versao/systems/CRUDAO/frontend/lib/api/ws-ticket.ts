import type { ApiError } from "@/lib/types";

/**
 * Obtém um ticket de curta duração para autenticar o handshake WebSocket (STOMP).
 * POST /api/ws-ticket → backend POST /api/ws-ticket.
 *
 * Deve ser chamado imediatamente antes de cada tentativa de conexão (o ticket expira em ~30s
 * e é de uso único conceitual).
 */
export async function obterWsTicket(): Promise<string> {
  const res = await fetch("/api/ws-ticket", {
    method: "POST",
    credentials: "include",
  });

  if (!res.ok) {
    const error = (await res.json().catch(() => ({}))) as ApiError;
    throw new Error(error.message || "Erro ao obter ticket de WebSocket");
  }

  const data = (await res.json()) as { ticket: string };
  return data.ticket;
}
