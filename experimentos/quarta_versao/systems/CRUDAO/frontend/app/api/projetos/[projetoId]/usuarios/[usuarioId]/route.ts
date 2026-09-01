import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ projetoId: string; usuarioId: string }> }
) {
  try {
    const { projetoId, usuarioId } = await params;
    const response = await apiProxyFetch(`/api/projetos/${projetoId}/usuarios/${usuarioId}`, {
      method: "DELETE",
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(errorData, { status: response.status });
    }
    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("[DELETE /api/projetos/:projetoId/usuarios/:usuarioId]", error);
    return NextResponse.json({ error: "Erro ao remover usuário" }, { status: 500 });
  }
}
