import { NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

export async function POST(
  _request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;
    const response = await apiProxyFetch(`/api/projetos/${id}/finalizar`, { method: "POST" });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(errorData, { status: response.status });
    }
    return NextResponse.json(await response.json());
  } catch (error) {
    console.error("[POST /api/admin/projeto/:id/finalizar]", error);
    return NextResponse.json({ error: "Erro ao finalizar projeto" }, { status: 500 });
  }
}
