import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

export async function GET(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const tarefaId = params.id;
    const response = await apiProxyFetch(`/api/tarefas/${tarefaId}/auditoria`, {
      method: "GET",
    });

    if (!response.ok) {
      return NextResponse.json(
        { error: "Erro ao obter auditoria" },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("[GET /api/tarefas/:id/auditoria]", error);
    return NextResponse.json(
      { error: "Erro ao obter auditoria" },
      { status: 500 }
    );
  }
}
