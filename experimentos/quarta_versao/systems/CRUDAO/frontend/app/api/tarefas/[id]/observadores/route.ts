import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id: tarefaId } = await params;
    const body = await request.json();

    const response = await apiProxyFetch(
      `/api/tarefas/${tarefaId}/observadores`,
      {
        method: "POST",
        body: JSON.stringify(body),
      }
    );

    if (!response.ok) {
      const errorData = await response.json();
      return NextResponse.json(errorData, { status: response.status });
    }

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("[POST /api/tarefas/:id/observadores]", error);
    return NextResponse.json(
      { error: "Erro ao adicionar observador" },
      { status: 500 }
    );
  }
}
