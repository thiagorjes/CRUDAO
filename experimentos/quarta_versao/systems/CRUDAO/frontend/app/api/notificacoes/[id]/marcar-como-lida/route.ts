import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

/**
 * PUT /api/notificacoes/{id}/marcar-como-lida
 * Proxy para PUT /api/notificacoes/{id}/marcar-como-lida do backend.
 * Autorização (notificação pertence ao usuário) é validada no backend.
 */
export async function PUT(
  _request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;

    const response = await apiProxyFetch(
      `/api/notificacoes/${id}/marcar-como-lida`,
      { method: "PUT" }
    );

    if (!response.ok) {
      return NextResponse.json(
        { message: "Erro ao marcar notificação como lida" },
        { status: response.status }
      );
    }

    return new NextResponse(null, { status: 204 });
  } catch (error) {
    console.error("[PUT /api/notificacoes/:id/marcar-como-lida]", error);
    return NextResponse.json(
      { message: "Erro ao marcar notificação como lida" },
      { status: 500 }
    );
  }
}
