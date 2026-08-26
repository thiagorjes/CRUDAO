import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** POST /api/projetos/{id}/raias — proxy de criação de raia (RF-011, TASK-07.4). */
export async function POST(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/projetos/${id}/raias`, "POST");
}
