import { NextRequest } from "next/server";
import { forwardToBackend } from "@/lib/proxy";

/**
 * GET /api/notificacoes — proxy da lista de notificações do usuário autenticado (RF-005,
 * TASK-07.7). `forwardToBackend` não repassa query string sozinho — montada aqui explicitamente
 * (achado de code review I1, mesmo padrão de `usuarios/buscar/route.ts` da TASK-07.5).
 */
export async function GET(req: NextRequest) {
  const apenasNaoLidas = req.nextUrl.searchParams.get("apenasNaoLidas");
  const query = apenasNaoLidas ? `?apenasNaoLidas=${encodeURIComponent(apenasNaoLidas)}` : "";
  return forwardToBackend(req, `/api/notificacoes${query}`, "GET");
}
