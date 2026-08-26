import { apiFetchJson } from "@/lib/api";
import { obterMe } from "@/lib/me";

type ProjetoResponse = {
  id: string;
  nome: string;
  descricao: string | null;
  status: "ATIVO" | "FINALIZADO";
  finalizadoEm: string | null;
};

/** TL-02 — Lista de Projetos (docs/design/kanban-tarefas/prototypes/tl-02-lista-projetos.html). */
export default async function ProjetosPage() {
  const [projetos, usuario] = await Promise.all([
    apiFetchJson<ProjetoResponse[]>("/api/projetos"),
    obterMe(),
  ]);

  const papeisPorProjeto = new Map(usuario.projetos.map((p) => [p.projetoId, p.papeis]));

  return (
    <>
      <div className="page-header">
        <h1>Projetos</h1>
      </div>

      {projetos.length === 0 ? (
        <div className="empty-state">
          <p>Você ainda não tem projetos associados.</p>
        </div>
      ) : (
        <section aria-label="Lista de projetos" className="project-grid">
          {projetos.map((projeto) => {
            const papeis = papeisPorProjeto.get(projeto.id) ?? [];
            const ehAdmin = papeis.includes("admin");
            const ativo = projeto.status === "ATIVO";
            return (
              <article className="card project-card" key={projeto.id}>
                <h3>{projeto.nome}</h3>
                {projeto.descricao && <p className="text-secondary">{projeto.descricao}</p>}
                <span className={ativo ? "badge badge-success" : "badge badge-neutro"}>
                  {ativo ? "Ativo" : "Finalizado"}
                </span>
                <div className="project-card__acoes">
                  <a className="btn btn-outline" href={`/projetos/${projeto.id}/board`}>
                    Board
                  </a>
                  <a className="btn btn-outline" href={`/projetos/${projeto.id}/dashboard`}>
                    Dashboard
                  </a>
                  {ehAdmin && (
                    <a className="btn btn-outline" href={`/projetos/${projeto.id}/admin`}>
                      Admin
                    </a>
                  )}
                </div>
              </article>
            );
          })}
        </section>
      )}
    </>
  );
}
