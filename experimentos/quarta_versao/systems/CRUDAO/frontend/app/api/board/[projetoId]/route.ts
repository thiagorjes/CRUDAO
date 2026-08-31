import { apiProxyFetch } from "@/lib/api";

/**
 * GET /api/board/{projetoId}
 * Proxy para GET /api/projetos/{projetoId}/board do backend
 */
export async function GET(
  request: Request,
  { params }: { params: { projetoId: string } }
) {
  const { projetoId } = params;

  const res = await apiProxyFetch(
    `/api/projetos/${projetoId}/board`,
    { method: "GET" }
  );

  if (!res.ok) {
    return new Response(res.body, { status: res.status });
  }

  return new Response(res.body, {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}
