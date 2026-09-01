import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id: tarefaId } = await params;
    const body = await request.json();

    const response = await apiProxyFetch(`/api/tarefas/${tarefaId}/mover`, {
      method: "POST",
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(errorData, { status: response.status });
    }

    // Backend responde 204 No Content (TarefaController.moverTarefa) — sem corpo para parsear.
    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("[POST /api/tarefas/:id/mover]", error);
    return NextResponse.json(
      { error: "Erro ao mover tarefa" },
      { status: 500 }
    );
  }
}
