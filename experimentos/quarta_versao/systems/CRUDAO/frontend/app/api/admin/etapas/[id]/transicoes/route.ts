import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

/** PUT /api/admin/etapas/{id}/transicoes — proxy para PUT /api/etapas/{id}/transicoes. */
export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;
    const body = await request.json();
    const response = await apiProxyFetch(`/api/etapas/${id}/transicoes`, {
      method: "PUT",
      body: JSON.stringify(body),
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(errorData, { status: response.status });
    }
    return NextResponse.json(await response.json());
  } catch (error) {
    console.error("[PUT /api/admin/etapas/:id/transicoes]", error);
    return NextResponse.json({ error: "Erro ao atualizar transições" }, { status: 500 });
  }
}
