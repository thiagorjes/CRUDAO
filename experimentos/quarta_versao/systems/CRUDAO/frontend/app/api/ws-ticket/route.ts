import { NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

/**
 * POST /api/ws-ticket
 * Proxy para POST /api/ws-ticket do backend — emite um ticket de curta duração para o
 * handshake WebSocket (board e notificações). O access token nunca é exposto ao JS.
 */
export async function POST() {
  try {
    const response = await apiProxyFetch("/api/ws-ticket", { method: "POST" });

    if (!response.ok) {
      return NextResponse.json(
        { message: "Erro ao emitir ticket de WebSocket" },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("[POST /api/ws-ticket]", error);
    return NextResponse.json(
      { message: "Erro ao emitir ticket de WebSocket" },
      { status: 500 }
    );
  }
}
