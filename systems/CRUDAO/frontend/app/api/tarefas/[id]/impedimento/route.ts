import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** POST/DELETE /api/tarefas/{id}/impedimento — proxy de marcar/desmarcar impedimento (RF-004). */
export async function POST(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/tarefas/${id}/impedimento`, "POST");
}

export async function DELETE(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/tarefas/${id}/impedimento`, "DELETE");
}
