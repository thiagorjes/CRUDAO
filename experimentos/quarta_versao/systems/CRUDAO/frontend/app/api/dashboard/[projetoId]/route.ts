import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ projetoId: string }> }
) {
  try {
    const { projetoId } = await params;

    const response = await apiProxyFetch(
      `/api/projetos/${projetoId}/dashboard`,
      { method: "GET" }
    );

    if (!response.ok) {
      const msg =
        response.status === 403
          ? "Sem acesso ao dashboard deste projeto"
          : response.status === 404
          ? "Projeto não encontrado"
          : "Erro ao obter dashboard";
      return NextResponse.json({ message: msg }, { status: response.status });
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("[GET /api/dashboard/:projetoId]", error);
    return NextResponse.json(
      { message: "Erro ao obter dashboard" },
      { status: 500 }
    );
  }
}
