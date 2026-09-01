import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ projetoId: string }> }
) {
  try {
    const { projetoId } = await params;
    const response = await apiProxyFetch(`/api/projetos/${projetoId}/usuarios`, { method: "GET" });
    if (!response.ok) {
      return NextResponse.json(
        { error: "Erro ao obter usuários do projeto" },
        { status: response.status }
      );
    }
    return NextResponse.json(await response.json());
  } catch (error) {
    console.error("[GET /api/projetos/:projetoId/usuarios]", error);
    return NextResponse.json({ error: "Erro ao obter usuários do projeto" }, { status: 500 });
  }
}

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ projetoId: string }> }
) {
  try {
    const { projetoId } = await params;
    const body = await request.json();
    const response = await apiProxyFetch(`/api/projetos/${projetoId}/usuarios`, {
      method: "POST",
      body: JSON.stringify(body),
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(errorData, { status: response.status });
    }
    return NextResponse.json({ success: true }, { status: 201 });
  } catch (error) {
    console.error("[POST /api/projetos/:projetoId/usuarios]", error);
    return NextResponse.json({ error: "Erro ao associar usuário" }, { status: 500 });
  }
}
