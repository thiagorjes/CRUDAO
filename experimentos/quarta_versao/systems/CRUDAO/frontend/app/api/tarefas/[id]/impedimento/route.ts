import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id: tarefaId } = await params;

    const response = await apiProxyFetch(`/api/tarefas/${tarefaId}/impedimento`, {
      method: "POST",
    });

    if (!response.ok) {
      const errorData = await response.json();
      return NextResponse.json(errorData, { status: response.status });
    }

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("[POST /api/tarefas/:id/impedimento]", error);
    return NextResponse.json(
      { error: "Erro ao marcar impedimento" },
      { status: 500 }
    );
  }
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id: tarefaId } = await params;

    const response = await apiProxyFetch(`/api/tarefas/${tarefaId}/impedimento`, {
      method: "DELETE",
    });

    if (!response.ok) {
      const errorData = await response.json();
      return NextResponse.json(errorData, { status: response.status });
    }

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("[DELETE /api/tarefas/:id/impedimento]", error);
    return NextResponse.json(
      { error: "Erro ao remover impedimento" },
      { status: 500 }
    );
  }
}
