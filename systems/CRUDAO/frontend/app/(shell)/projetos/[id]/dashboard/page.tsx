import { apiFetch, apiFetchJson } from "@/lib/api";
import { DashboardResponse } from "@/lib/dashboard";
import { ProjetoResponse } from "@/lib/admin";
import { DashboardClient } from "@/components/dashboard/DashboardClient";

/**
 * TL — Dashboard de lead-time (RF-007, TASK-07.6). Acessível a qualquer usuário vinculado ao
 * projeto — inclusive papel `gestor`, sem `tarefa:gerenciar`/permissão de execução (critério de
 * aceite da task, contrato `dashboard-notificacoes.md`). Permanece acessível com projeto
 * finalizado (RN-015 — leitura nunca é bloqueada).
 */
export default async function DashboardPage({ params }: { params: Promise<{ id: string }> }) {
  const { id: projetoId } = await params;

  const [projetos, dashboardRes] = await Promise.all([
    apiFetchJson<ProjetoResponse[]>("/api/projetos"),
    apiFetch(`/api/projetos/${projetoId}/dashboard`),
  ]);

  const projeto = projetos.find((p) => p.id === projetoId);
  if (!projeto) {
    return <p className="text-secondary">Projeto não encontrado.</p>;
  }

  const voltar = (
    <div className="page-header" style={{ marginBottom: 0 }}>
      <a href={`/projetos/${projetoId}/board`} className="btn btn-text" style={{ paddingLeft: 0 }}>
        ← Board
      </a>
    </div>
  );

  if (dashboardRes.status === 403) {
    return (
      <>
        {voltar}
        <div className="empty-state">Você não tem acesso a este projeto.</div>
      </>
    );
  }
  if (!dashboardRes.ok) {
    throw new Error(`Chamada a /api/projetos/${projetoId}/dashboard falhou com status ${dashboardRes.status}`);
  }

  const dashboard: DashboardResponse = await dashboardRes.json();

  return (
    <>
      {voltar}
      <h1>Dashboard — {projeto.nome}</h1>
      <DashboardClient dashboard={dashboard} />
    </>
  );
}
