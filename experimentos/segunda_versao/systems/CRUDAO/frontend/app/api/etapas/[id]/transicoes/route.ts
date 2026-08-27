import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/** PUT /api/etapas/{id}/transicoes — proxy de substituição das transições de saída (RF-010, RN-003, TASK-07.4). */
export async function PUT(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return forwardToBackend(req, `/api/etapas/${id}/transicoes`, "PUT");
}
