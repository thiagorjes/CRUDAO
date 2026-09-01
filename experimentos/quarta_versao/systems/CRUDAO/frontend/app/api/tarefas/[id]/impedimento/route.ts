import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

/**
 * O backend (`TarefaController.marcarImpedimento`/`desmarcarImpedimento`) exige `projetoId` como
 * query param (valida permissão + projeto ativo) — precisa ser repassado em ambos os métodos.
 */

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id: tarefaId } = await params;
    const projetoId = request.nextUrl.searchParams.get("projetoId");
    if (!projetoId) {
      return NextResponse.json({ error: "projetoId é obrigatório" }, { status: 400 });
    }

    const response = await apiProxyFetch(
      `/api/tarefas/${tarefaId}/impedimento?projetoId=${encodeURIComponent(projetoId)}`,
      { method: "POST" }
    );

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
    const projetoId = request.nextUrl.searchParams.get("projetoId");
    if (!projetoId) {
      return NextResponse.json({ error: "projetoId é obrigatório" }, { status: 400 });
    }

    const response = await apiProxyFetch(
      `/api/tarefas/${tarefaId}/impedimento?projetoId=${encodeURIComponent(projetoId)}`,
      { method: "DELETE" }
    );

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
