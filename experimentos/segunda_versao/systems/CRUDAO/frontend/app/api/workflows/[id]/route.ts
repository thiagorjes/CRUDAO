import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** DELETE /api/workflows/{id} — proxy (RN-005, TASK-07.4). */
export async function DELETE(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/workflows/${id}`, "DELETE");
}
