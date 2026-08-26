import { apiFetchJson } from "@/lib/api";
import { ProjetoResponse, RaiaResponse, WorkflowResponse } from "@/lib/admin";
import { AdminClient } from "@/components/admin/AdminClient";

/** TL-08 — Admin de Projeto (RF-008/009/010/011, TASK-07.4). */
export default async function AdminPage({ params }: { params: Promise<{ id: string }> }) {
  const { id: projetoId } = await params;

  const [projetos, workflows, raias] = await Promise.all([
    apiFetchJson<ProjetoResponse[]>("/api/projetos"),
    apiFetchJson<WorkflowResponse[]>(`/api/projetos/${projetoId}/workflows`),
    apiFetchJson<RaiaResponse[]>(`/api/projetos/${projetoId}/raias`),
  ]);

  const projeto = projetos.find((p) => p.id === projetoId);
  if (!projeto) {
    return <p className="text-secondary">Projeto não encontrado.</p>;
  }

  return (
    <>
      <a href={`/projetos/${projetoId}/board`} className="btn btn-text" style={{ paddingLeft: 0 }}>
        ← Board
      </a>
      <AdminClient projeto={projeto} workflow={workflows[0] ?? null} raias={raias} />
    </>
  );
}
