import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** PUT /api/projetos/{id} — proxy de edição de nome/descrição (RF-008, TASK-07.4). */
export async function PUT(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/projetos/${id}`, "PUT");
}
