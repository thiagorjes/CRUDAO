import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** DELETE /api/projetos/{id}/usuarios/{usuarioId} — proxy de remoção de vínculo (RF-015, TASK-07.5). */
export async function DELETE(
  req: NextRequest,
  { params }: { params: Promise<{ id: string; usuarioId: string }> },
) {
  const { id, usuarioId } = await params;
  return forwardToBackend(req, `/api/projetos/${id}/usuarios/${usuarioId}`, "DELETE");
}
