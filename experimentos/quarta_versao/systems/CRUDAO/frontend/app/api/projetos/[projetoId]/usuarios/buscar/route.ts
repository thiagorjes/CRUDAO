import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

/** GET /api/projetos/{projetoId}/usuarios/buscar?q= — autocomplete de usuários não associados. */
export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ projetoId: string }> }
) {
  try {
    const { projetoId } = await params;
    const q = request.nextUrl.searchParams.get("q") ?? "";
    const response = await apiProxyFetch(
      `/api/projetos/${projetoId}/usuarios/buscar?q=${encodeURIComponent(q)}`,
      { method: "GET" }
    );
    if (!response.ok) {
      return NextResponse.json({ error: "Erro na busca de usuários" }, { status: response.status });
    }
    return NextResponse.json(await response.json());
  } catch (error) {
    console.error("[GET /api/projetos/:projetoId/usuarios/buscar]", error);
    return NextResponse.json({ error: "Erro na busca de usuários" }, { status: 500 });
  }
}
