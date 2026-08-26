import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** POST /api/projetos/{id}/finalizar — proxy (RF-008, RN-015, TASK-07.4). */
export async function POST(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/projetos/${id}/finalizar`, "POST");
}
