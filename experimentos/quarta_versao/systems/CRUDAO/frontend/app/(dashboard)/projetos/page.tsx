import Link from "next/link";
import { obterMe } from "@/lib/me";
import { apiFetchJson } from "@/lib/api";
import type { ProjetoResumo } from "@/lib/types";
import NovoProjetoButton from "@/components/projetos/NovoProjetoButton";

/** TL-02 — Lista de Projetos (RF-008) */
export default async function ProjetosPage() {
  const me = await obterMe();

  // adminGlobal não tem vínculo com todos os projetos; usa a listagem completa do backend.
  let projetosAdmin: ProjetoResumo[] = [];
  if (me.adminGlobal) {
    try {
      projetosAdmin = await apiFetchJson<ProjetoResumo[]>("/api/projetos");
    } catch {
      projetosAdmin = [];
    }
  }

  const vazio = me.adminGlobal ? projetosAdmin.length === 0 : me.projetos.length === 0;

  return (
    <div>
      <div
        className="page-header"
        style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "var(--space-md)" }}
      >
        <h1>{me.adminGlobal ? "Projetos" : "Meus Projetos"}</h1>
        {me.adminGlobal && <NovoProjetoButton />}
      </div>

      {vazio ? (
        <div className="empty-state">
          <p>
            {me.adminGlobal
              ? "Nenhum projeto ainda. Use “Novo projeto” para criar o primeiro."
              : "Você não está vinculado a nenhum projeto ainda."}
          </p>
        </div>
      ) : me.adminGlobal ? (
        <div className="project-grid">
          {projetosAdmin.map((p) => (
            <div key={p.id} className="card project-card">
              <h3>{p.nome}</h3>
              <p className="text-secondary">
                {p.status === "FINALIZADO" ? "Finalizado" : "Ativo"}
                {p.descricao ? ` — ${p.descricao}` : ""}
              </p>
              <div className="project-card__acoes">
                <Link href={`/projetos/${p.id}/board`} className="btn btn-primary">
                  Board
                </Link>
                <Link href={`/projetos/${p.id}/admin`} className="btn btn-outline">
                  Admin
                </Link>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="project-grid">
          {me.projetos.map((projeto) => (
            <div key={projeto.projetoId} className="card project-card">
              <h3>Projeto {projeto.projetoId.substring(0, 8)}</h3>
              <p className="text-secondary">
                {projeto.papeis.length > 0
                  ? `Papéis: ${projeto.papeis.join(", ")}`
                  : "Sem papéis atribuídos"}
              </p>

              <div className="project-card__acoes">
                <Link href={`/projetos/${projeto.projetoId}/board`} className="btn btn-primary">
                  Board
                </Link>
                {projeto.papeis.some((r) => ["admin", "project_admin"].includes(r)) && (
                  <Link href={`/projetos/${projeto.projetoId}/admin`} className="btn btn-outline">
                    Admin
                  </Link>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
