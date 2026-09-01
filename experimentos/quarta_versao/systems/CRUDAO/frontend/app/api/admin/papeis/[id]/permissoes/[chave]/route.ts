import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

/** PUT /api/admin/papeis/{id}/permissoes/{chave} — proxy para PUT /api/papeis/{id}/permissoes/{chave}. */
export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string; chave: string }> }
) {
  try {
    const { id, chave } = await params;
    const body = await request.json();
    const response = await apiProxyFetch(`/api/papeis/${id}/permissoes/${chave}`, {
      method: "PUT",
      body: JSON.stringify(body),
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(errorData, { status: response.status });
    }
    return NextResponse.json(await response.json());
  } catch (error) {
    console.error("[PUT /api/admin/papeis/:id/permissoes/:chave]", error);
    return NextResponse.json({ error: "Erro ao atualizar permissão" }, { status: 500 });
  }
}
