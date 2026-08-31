import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id: tarefaId } = await params;
    const response = await apiProxyFetch(`/api/tarefas/${tarefaId}`, {
      method: "GET",
    });

    if (!response.ok) {
      return NextResponse.json(
        { error: "Erro ao obter tarefa" },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("[GET /api/tarefas/:id]", error);
    return NextResponse.json(
      { error: "Erro ao obter tarefa" },
      { status: 500 }
    );
  }
}

export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id: tarefaId } = await params;
    const body = await request.json();

    const response = await apiProxyFetch(`/api/tarefas/${tarefaId}`, {
      method: "PUT",
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      const errorData = await response.json();
      return NextResponse.json(errorData, { status: response.status });
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("[PUT /api/tarefas/:id]", error);
    return NextResponse.json(
      { error: "Erro ao editar tarefa" },
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
    const response = await apiProxyFetch(`/api/tarefas/${tarefaId}`, {
      method: "DELETE",
    });

    if (!response.ok) {
      return NextResponse.json(
        { error: "Erro ao excluir tarefa" },
        { status: response.status }
      );
    }

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("[DELETE /api/tarefas/:id]", error);
    return NextResponse.json(
      { error: "Erro ao excluir tarefa" },
      { status: 500 }
    );
  }
}
