import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** PUT /api/etapas/{id} — proxy de edição (nome/ordem/etapaFinal, RF-009, TASK-07.4). */
export async function PUT(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/etapas/${id}`, "PUT");
}

/** DELETE /api/etapas/{id} — proxy (RN-005, TASK-07.4). */
export async function DELETE(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/etapas/${id}`, "DELETE");
}
