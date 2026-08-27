import { apiFetch, apiFetchJson } from "@/lib/api";
import { ProjetoResponse } from "@/lib/admin";
import { obterMe } from "@/lib/me";
import { PapelResponse, UsuarioProjetoResponse } from "@/lib/papeis";
import { PapeisClient } from "@/components/admin/PapeisClient";

/**
 * TL-09/TL-10 — Admin de Papéis/Permissões/Usuários (RF-013/015/016, TASK-07.5).
 * GETs de leitura (`papeis`, `usuarios`) exigem só vínculo ao projeto — qualquer membro pode ver;
 * mutações exigem `papel:administrar`, tratadas com 403 explícito nos formulários/ações da UI
 * (mesmo padrão de "sem permissão" de TASK-07.4).
 */
export default async function AdminPapeisPage({ params }: { params: Promise<{ id: string }> }) {
  const { id: projetoId } = await params;

  const [projetos, papeisRes, usuariosRes, me] = await Promise.all([
    apiFetchJson<ProjetoResponse[]>("/api/projetos"),
    apiFetch(`/api/projetos/${projetoId}/papeis`),
    apiFetch(`/api/projetos/${projetoId}/usuarios`),
    obterMe(),
  ]);

  const projeto = projetos.find((p) => p.id === projetoId);
  if (!projeto) {
    return <p className="text-secondary">Projeto não encontrado.</p>;
  }

  const voltar = (
    <a href={`/projetos/${projetoId}/admin`} className="btn btn-text" style={{ paddingLeft: 0 }}>
      ← Admin
    </a>
  );

  if (papeisRes.status === 403 || usuariosRes.status === 403) {
    return (
      <>
        {voltar}
        <div className="empty-state">Você não tem permissão para ver papéis/usuários deste projeto.</div>
      </>
    );
  }
  if (!papeisRes.ok) {
    throw new Error(`Chamada a /api/projetos/${projetoId}/papeis falhou com status ${papeisRes.status}`);
  }
  if (!usuariosRes.ok) {
    throw new Error(`Chamada a /api/projetos/${projetoId}/usuarios falhou com status ${usuariosRes.status}`);
  }

  const papeis: PapelResponse[] = await papeisRes.json();
  const usuarios: UsuarioProjetoResponse[] = await usuariosRes.json();
  const usuarioAutenticadoId = me.projetos.some((p) => p.projetoId === projetoId) ? me.id : null;

  return (
    <>
      {voltar}
      <PapeisClient
        projeto={projeto}
        papeis={papeis}
        usuarios={usuarios}
        usuarioAutenticadoId={usuarioAutenticadoId}
      />
    </>
  );
}
