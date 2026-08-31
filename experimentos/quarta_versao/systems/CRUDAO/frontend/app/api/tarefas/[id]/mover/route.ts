import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

export async function POST(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const tarefaId = params.id;
    const body = await request.json();

    const response = await apiProxyFetch(`/api/tarefas/${tarefaId}/mover`, {
      method: "POST",
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      const errorData = await response.json();
      return NextResponse.json(errorData, { status: response.status });
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("[POST /api/tarefas/:id/mover]", error);
    return NextResponse.json(
      { error: "Erro ao mover tarefa" },
      { status: 500 }
    );
  }
}
