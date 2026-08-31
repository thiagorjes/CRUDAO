import { apiProxyFetch } from "@/lib/api";

/**
 * POST /api/tarefas/{tarefaId}/mover
 * Proxy para POST /api/tarefas/{tarefaId}/mover do backend
 */
export async function POST(
  request: Request,
  { params }: { params: { tarefaId: string } }
) {
  const { tarefaId } = params;
  const body = await request.json();

  const res = await apiProxyFetch(`/api/tarefas/${tarefaId}/mover`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    return new Response(res.body, { status: res.status });
  }

  return new Response(res.body, {
    status: res.status,
    headers: { "Content-Type": "application/json" },
  });
}
