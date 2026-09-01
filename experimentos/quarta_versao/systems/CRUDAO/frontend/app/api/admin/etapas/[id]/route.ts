import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;
    const body = await request.json();
    const response = await apiProxyFetch(`/api/etapas/${id}`, {
      method: "PUT",
      body: JSON.stringify(body),
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(errorData, { status: response.status });
    }
    return NextResponse.json(await response.json());
  } catch (error) {
    console.error("[PUT /api/admin/etapas/:id]", error);
    return NextResponse.json({ error: "Erro ao atualizar etapa" }, { status: 500 });
  }
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;
    const response = await apiProxyFetch(`/api/etapas/${id}`, { method: "DELETE" });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(errorData, { status: response.status });
    }
    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("[DELETE /api/admin/etapas/:id]", error);
    return NextResponse.json({ error: "Erro ao excluir etapa" }, { status: 500 });
  }
}
