import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

/**
 * GET /api/notificacoes
 * Proxy para GET /api/notificacoes do backend (notificações não lidas do usuário autenticado).
 */
export async function GET(_request: NextRequest) {
  try {
    const response = await apiProxyFetch("/api/notificacoes", { method: "GET" });

    if (!response.ok) {
      return NextResponse.json(
        { message: "Erro ao obter notificações" },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("[GET /api/notificacoes]", error);
    return NextResponse.json(
      { message: "Erro ao obter notificações" },
      { status: 500 }
    );
  }
}
