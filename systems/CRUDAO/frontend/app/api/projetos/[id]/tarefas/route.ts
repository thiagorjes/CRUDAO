import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** POST /api/projetos/{id}/tarefas — proxy de criação de card pelo board (RF-018, TASK-07.2). */
export async function POST(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/projetos/${id}/tarefas`, "POST");
}
