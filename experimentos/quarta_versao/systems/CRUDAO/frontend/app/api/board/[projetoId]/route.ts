import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api";

/**
 * GET /api/board/{projetoId}
 * Proxy para GET /api/projetos/{projetoId}/board do backend
 */
export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ projetoId: string }> }
) {
  try {
    const { projetoId } = await params;

    const res = await apiProxyFetch(
      `/api/projetos/${projetoId}/board`,
      { method: "GET" }
    );

    if (!res.ok) {
      return NextResponse.json(
        { error: "Erro ao obter board" },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("[GET /api/board/:projetoId]", error);
    return NextResponse.json(
      { error: "Erro ao obter board" },
      { status: 500 }
    );
  }
}
