import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** POST /api/notificacoes/{id}/lida — proxy para marcar notificação como lida (RF-005, TASK-07.7). */
export async function POST(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/notificacoes/${id}/lida`, "POST");
}
