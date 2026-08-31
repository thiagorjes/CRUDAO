import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api";

/**
 * POST /api/board/{projetoId}/tarefas
 * Proxy para POST /api/projetos/{projetoId}/tarefas do backend
 */
export async function POST(
  request: NextRequest,
  { params }: { params: { projetoId: string } }
) {
  try {
    const { projetoId } = params;
    const body = await request.json();

    const res = await apiProxyFetch(`/api/projetos/${projetoId}/tarefas`, {
      method: "POST",
      body: JSON.stringify(body),
    });

    if (!res.ok) {
      const errorData = await res.json();
      return NextResponse.json(errorData, { status: res.status });
    }

    const data = await res.json();
    return NextResponse.json(data, { status: res.status });
  } catch (error) {
    console.error("[POST /api/board/:projetoId/tarefas]", error);
    return NextResponse.json(
      { error: "Erro ao criar tarefa" },
      { status: 500 }
    );
  }
}
