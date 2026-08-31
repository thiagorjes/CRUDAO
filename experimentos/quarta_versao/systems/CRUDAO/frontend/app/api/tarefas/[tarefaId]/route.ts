import { apiProxyFetch } from "@/lib/api";

/**
 * DELETE /api/tarefas/{tarefaId}
 * Proxy para DELETE /api/tarefas/{tarefaId} do backend
 */
export async function DELETE(
  request: Request,
  { params }: { params: { tarefaId: string } }
) {
  const { tarefaId } = params;

  const res = await apiProxyFetch(`/api/tarefas/${tarefaId}`, {
    method: "DELETE",
  });

  if (!res.ok) {
    return new Response(res.body, { status: res.status });
  }

  return new Response(null, { status: 204 });
}
