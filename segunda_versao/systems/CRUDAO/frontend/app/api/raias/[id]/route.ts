import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** PUT /api/raias/{id} — proxy de edição (RF-011, TASK-07.4). */
export async function PUT(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/raias/${id}`, "PUT");
}

/** DELETE /api/raias/{id} — proxy (RN-005, TASK-07.4). */
export async function DELETE(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/raias/${id}`, "DELETE");
}
