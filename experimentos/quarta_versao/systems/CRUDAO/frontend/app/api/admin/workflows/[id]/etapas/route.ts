import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

/** POST /api/admin/workflows/{id}/etapas — proxy para POST /api/workflows/{id}/etapas. */
export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;
    const body = await request.json();
    const response = await apiProxyFetch(`/api/workflows/${id}/etapas`, {
      method: "POST",
      body: JSON.stringify(body),
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(errorData, { status: response.status });
    }
    return NextResponse.json(await response.json(), { status: 201 });
  } catch (error) {
    console.error("[POST /api/admin/workflows/:id/etapas]", error);
    return NextResponse.json({ error: "Erro ao criar etapa" }, { status: 500 });
  }
}
