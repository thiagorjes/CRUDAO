import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** POST /api/projetos/{id}/usuarios — proxy de associação usuário↔projeto↔papel (RF-015, TASK-07.5). */
export async function POST(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/projetos/${id}/usuarios`, "POST");
}
