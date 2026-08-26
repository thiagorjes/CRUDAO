import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** POST /api/workflows/{id}/etapas — proxy de criação de etapa (RF-009, TASK-07.4). */
export async function POST(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/workflows/${id}/etapas`, "POST");
}
