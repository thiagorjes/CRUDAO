import { apiProxyFetch } from "@/lib/api";

/**
 * POST /api/board/{projetoId}/tarefas
 * Proxy para POST /api/projetos/{projetoId}/tarefas do backend
 */
export async function POST(
  request: Request,
  { params }: { params: { projetoId: string } }
) {
  const { projetoId } = params;
  const body = await request.json();

  const res = await apiProxyFetch(`/api/projetos/${projetoId}/tarefas`, {
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
