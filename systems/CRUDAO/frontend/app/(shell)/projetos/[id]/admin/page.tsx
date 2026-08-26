import { apiFetch, apiFetchJson } from "@/lib/api";
import { ProjetoResponse, RaiaResponse, WorkflowResponse } from "@/lib/admin";
import { AdminClient } from "@/components/admin/AdminClient";

/**
 * TL-08 — Admin de Projeto (RF-008/009/010/011, TASK-07.4).
 * `workflow:administrar` é exigida por todo o contrato de workflows.md (incl. GET) — sem essa
 * permissão a página trata o 403 explicitamente em vez de deixar o error boundary genérico do
 * shell disparar (achado de code review, agent QA — mesmo padrão de "sem acesso" já usado para
 * auditoria em TASK-07.3).
 */
export default async function AdminPage({ params }: { params: Promise<{ id: string }> }) {
  const { id: projetoId } = await params;

  const [projetos, workflowsRes, raias] = await Promise.all([
    apiFetchJson<ProjetoResponse[]>("/api/projetos"),
    apiFetch(`/api/projetos/${projetoId}/workflows`),
    apiFetchJson<RaiaResponse[]>(`/api/projetos/${projetoId}/raias`),
  ]);

  const projeto = projetos.find((p) => p.id === projetoId);
  if (!projeto) {
    return <p className="text-secondary">Projeto não encontrado.</p>;
  }

  const voltar = (
    <a href={`/projetos/${projetoId}/board`} className="btn btn-text" style={{ paddingLeft: 0 }}>
      ← Board
    </a>
  );

  if (workflowsRes.status === 403) {
    return (
      <>
        {voltar}
        <div className="empty-state">
          Você não tem permissão para administrar este projeto (requer &quot;workflow:administrar&quot;).
        </div>
      </>
    );
  }
  if (!workflowsRes.ok) {
    throw new Error(`Chamada a /api/projetos/${projetoId}/workflows falhou com status ${workflowsRes.status}`);
  }

  const workflows: WorkflowResponse[] = await workflowsRes.json();

  return (
    <>
      {voltar}
      <AdminClient projeto={projeto} workflow={workflows[0] ?? null} raias={raias} />
    </>
  );
}
