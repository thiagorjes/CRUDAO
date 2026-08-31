import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id: projetoId } = await params;
    const response = await apiProxyFetch(`/api/projetos/${projetoId}`, {
      method: "GET",
    });

    if (!response.ok) {
      return NextResponse.json(
        { error: "Erro ao obter projeto" },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("[GET /api/admin/projeto/:id]", error);
    return NextResponse.json(
      { error: "Erro ao obter projeto" },
      { status: 500 }
    );
  }
}

export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id: projetoId } = await params;
    const body = await request.json();

    const response = await apiProxyFetch(`/api/projetos/${projetoId}`, {
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
    console.error("[PUT /api/admin/projeto/:id]", error);
    return NextResponse.json(
      { error: "Erro ao atualizar projeto" },
      { status: 500 }
    );
  }
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id: projetoId } = await params;

    const response = await apiProxyFetch(`/api/projetos/${projetoId}`, {
      method: "DELETE",
    });

    if (!response.ok) {
      const errorData = await response.json();
      return NextResponse.json(errorData, { status: response.status });
    }

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("[DELETE /api/admin/projeto/:id]", error);
    return NextResponse.json(
      { error: "Erro ao deletar projeto" },
      { status: 500 }
    );
  }
}
