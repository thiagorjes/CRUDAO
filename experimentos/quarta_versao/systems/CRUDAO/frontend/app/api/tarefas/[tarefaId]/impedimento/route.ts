import { apiProxyFetch } from "@/lib/api";

/**
 * POST /api/tarefas/{tarefaId}/impedimento
 * Proxy para POST /api/tarefas/{tarefaId}/impedimento do backend
 */
export async function POST(
  request: Request,
  { params }: { params: { tarefaId: string } }
) {
  const { tarefaId } = params;

  const res = await apiProxyFetch(`/api/tarefas/${tarefaId}/impedimento`, {
    method: "POST",
  });

  if (!res.ok) {
    return new Response(res.body, { status: res.status });
  }

  return new Response(res.body, {
    status: res.status,
    headers: { "Content-Type": "application/json" },
  });
}

/**
 * DELETE /api/tarefas/{tarefaId}/impedimento
 * Proxy para DELETE /api/tarefas/{tarefaId}/impedimento do backend
 */
export async function DELETE(
  request: Request,
  { params }: { params: { tarefaId: string } }
) {
  const { tarefaId } = params;

  const res = await apiProxyFetch(`/api/tarefas/${tarefaId}/impedimento`, {
    method: "DELETE",
  });

  if (!res.ok) {
    return new Response(res.body, { status: res.status });
  }

  return new Response(null, { status: 204 });
}
