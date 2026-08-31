import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

export async function DELETE(
  request: NextRequest,
  { params }: { params: { id: string; usuarioId: string } }
) {
  try {
    const tarefaId = params.id;
    const usuarioId = params.usuarioId;

    const response = await apiProxyFetch(
      `/api/tarefas/${tarefaId}/observadores/${usuarioId}`,
      {
        method: "DELETE",
      }
    );

    if (!response.ok) {
      const errorData = await response.json();
      return NextResponse.json(errorData, { status: response.status });
    }

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("[DELETE /api/tarefas/:id/observadores/:usuarioId]", error);
    return NextResponse.json(
      { error: "Erro ao remover observador" },
      { status: 500 }
    );
  }
}
