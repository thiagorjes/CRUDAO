import { apiFetchJson } from "@/lib/api";
import { BoardResponse, MembroProjeto } from "@/lib/board";
import { BoardClient } from "@/components/board/BoardClient";

type ProjetoResponse = { id: string; nome: string; status: "ATIVO" | "FINALIZADO" };

/** TL-03 — Board (docs/design/kanban-tarefas/prototypes/tl-03-board-compacto.html), RF-001/002/004/018/019. */
export default async function BoardPage({ params }: { params: Promise<{ id: string }> }) {
  const { id: projetoId } = await params;

  const [board, membros, projetos] = await Promise.all([
    apiFetchJson<BoardResponse>(`/api/projetos/${projetoId}/board`),
    apiFetchJson<MembroProjeto[]>(`/api/projetos/${projetoId}/usuarios`),
    apiFetchJson<ProjetoResponse[]>("/api/projetos"),
  ]);

  const projeto = projetos.find((p) => p.id === projetoId);

  return (
    <>
      <div className="page-header">
        <div>
          <a href="/projetos" className="btn btn-text" style={{ paddingLeft: 0 }}>
            ← Projetos
          </a>
          <h1>Board — {projeto?.nome ?? "Projeto"}</h1>
        </div>
        <a className="btn btn-outline" href={`/projetos/${projetoId}/dashboard`}>
          Dashboard
        </a>
      </div>

      <BoardClient projetoId={projetoId} board={board} membros={membros} />
    </>
  );
}
