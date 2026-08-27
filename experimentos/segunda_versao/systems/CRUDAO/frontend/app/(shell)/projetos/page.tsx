import { apiFetchJson } from "@/lib/api";
import { obterMe } from "@/lib/me";
import { ProjetosClient } from "@/components/projetos/ProjetosClient";

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
    <ProjetosClient
      projetos={projetos}
      papeisPorProjeto={papeisPorProjeto}
      podeCriar={usuario.adminGlobal}
    />
  );
}
