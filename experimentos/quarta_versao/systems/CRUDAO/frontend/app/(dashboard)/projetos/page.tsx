import Link from "next/link";
import { obterMe } from "@/lib/me";

/** TL-02 — Lista de Projetos (RF-008) */
export default async function ProjetosPage() {
  const me = await obterMe();

  return (
    <div>
      <div className="page-header">
        <h1>Meus Projetos</h1>
      </div>

      {me.projetos.length === 0 ? (
        <div className="empty-state">
          <p>Você não está vinculado a nenhum projeto ainda.</p>
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
