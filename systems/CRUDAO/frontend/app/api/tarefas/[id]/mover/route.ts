import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** POST /api/tarefas/{id}/mover — proxy de movimentação entre etapas (RF-002, TASK-07.2). */
export async function POST(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/tarefas/${id}/mover`, "POST");
}
