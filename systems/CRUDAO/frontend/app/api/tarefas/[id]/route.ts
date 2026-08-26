import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** DELETE /api/tarefas/{id} — proxy de exclusão de card (RF-019, TASK-07.2). */
export async function DELETE(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/tarefas/${id}`, "DELETE");
}
