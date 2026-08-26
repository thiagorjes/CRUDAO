import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** PUT /api/papeis/{id}/permissoes/{chave} — proxy de toggle de permissão (RF-016, TASK-07.5). */
export async function PUT(req: NextRequest, { params }: { params: Promise<{ id: string; chave: string }> }) {
  const { id, chave } = await params;
  return forwardToBackend(req, `/api/papeis/${id}/permissoes/${chave}`, "PUT");
}
