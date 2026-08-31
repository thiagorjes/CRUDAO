import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ projetoId: string }> }
) {
  try {
    const { projetoId } = await params;

    const response = await apiProxyFetch(`/api/projetos/${projetoId}/usuarios`, {
      method: "GET",
    });

    if (!response.ok) {
      return NextResponse.json(
        { error: "Erro ao obter usuários do projeto" },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("[GET /api/projetos/:projetoId/usuarios]", error);
    return NextResponse.json(
      { error: "Erro ao obter usuários do projeto" },
      { status: 500 }
    );
  }
}
