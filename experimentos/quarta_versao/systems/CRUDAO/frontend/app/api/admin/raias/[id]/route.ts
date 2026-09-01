import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;
    const body = await request.json();
    const response = await apiProxyFetch(`/api/raias/${id}`, {
      method: "PUT",
      body: JSON.stringify(body),
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(errorData, { status: response.status });
    }
    return NextResponse.json(await response.json());
  } catch (error) {
    console.error("[PUT /api/admin/raias/:id]", error);
    return NextResponse.json({ error: "Erro ao atualizar raia" }, { status: 500 });
  }
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;
    const response = await apiProxyFetch(`/api/raias/${id}`, { method: "DELETE" });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(errorData, { status: response.status });
    }
    // Backend responde 204 No Content.
    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("[DELETE /api/admin/raias/:id]", error);
    return NextResponse.json({ error: "Erro ao excluir raia" }, { status: 500 });
  }
}
