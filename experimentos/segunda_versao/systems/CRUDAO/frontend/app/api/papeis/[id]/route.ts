import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** PUT /api/papeis/{id} — proxy de edição de nome do papel (RF-013, TASK-07.5). */
export async function PUT(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/papeis/${id}`, "PUT");
}

/** DELETE /api/papeis/{id} — proxy (RN-006 bloqueia papel protegido, RF-013). */
export async function DELETE(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/papeis/${id}`, "DELETE");
}
