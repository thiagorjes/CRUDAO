import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** DELETE /api/tarefas/{id}/observadores/{usuarioId} — proxy de remoção de observador (RF-005, TASK-07.3). */
export async function DELETE(
  req: NextRequest,
  { params }: { params: Promise<{ id: string; usuarioId: string }> },
) {
  const { id, usuarioId } = await params;
  return forwardToBackend(req, `/api/tarefas/${id}/observadores/${usuarioId}`, "DELETE");
}
