import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** POST /api/tarefas/{id}/observadores — proxy de adição de observador explícito (RF-005, TASK-07.3). */
export async function POST(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/tarefas/${id}/observadores`, "POST");
}
