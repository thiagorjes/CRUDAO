import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/**
 * GET /api/projetos/{id}/usuarios/buscar?q= — proxy do autocomplete de associação (RF-015,
 * TASK-07.5). `forwardToBackend` não repassa query string sozinho — montada aqui explicitamente.
 */
export async function GET(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const q = req.nextUrl.searchParams.get("q") ?? "";
  return forwardToBackend(req, `/api/projetos/${id}/usuarios/buscar?q=${encodeURIComponent(q)}`, "GET");
}
