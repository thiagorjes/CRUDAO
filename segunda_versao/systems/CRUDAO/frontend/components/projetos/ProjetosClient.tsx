"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { NovoProjetoModal } from "./NovoProjetoModal";

type ProjetoResponse = {
  id: string;
  nome: string;
  descricao: string | null;
  status: "ATIVO" | "FINALIZADO";
  finalizadoEm: string | null;
};

/** TL-02 — Lista de Projetos (RF-008: "+ Novo projeto"/"Criar primeiro projeto", só para adminGlobal). */
export function ProjetosClient({
  projetos,
  papeisPorProjeto,
  podeCriar,
}: {
  projetos: ProjetoResponse[];
  papeisPorProjeto: Map<string, string[]>;
  podeCriar: boolean;
}) {
  const router = useRouter();
  const [modalAberto, setModalAberto] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  function aoCriar() {
    setModalAberto(false);
    router.refresh();
  }

  return (
    <>
      <div className="page-header">
        <h1>Projetos</h1>
        {podeCriar && (
          <button className="btn btn-primary" type="button" onClick={() => setModalAberto(true)}>
            + Novo projeto
          </button>
        )}
      </div>

      {erro && (
        <div className="toast toast-error" role="alert" style={{ marginBottom: "16px" }}>
          {erro}
        </div>
      )}

      {projetos.length === 0 ? (
        <div className="empty-state">
          <p>Você ainda não tem projetos associados.</p>
          {podeCriar && (
            <button className="btn btn-primary" type="button" onClick={() => setModalAberto(true)}>
              Criar primeiro projeto
            </button>
          )}
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

      {modalAberto && (
        <NovoProjetoModal
          onFechar={() => setModalAberto(false)}
          onCriado={aoCriar}
          onErro={setErro}
        />
      )}
    </>
  );
}
