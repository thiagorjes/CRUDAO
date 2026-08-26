"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ProjetoResponse } from "@/lib/admin";
import { PapelResponse, UsuarioProjetoResponse } from "@/lib/papeis";
import { autorPossuiPapel, mensagemErroPapeis, ordenarPapeisPorNome, ordenarUsuariosPorNome, papeisAssociaveis } from "@/lib/papeis-logic";
import { AssociarUsuarioModal } from "./AssociarUsuarioModal";
import { ConfirmModal } from "./ConfirmModal";
import { PapelModal } from "./PapelModal";
import { PermissoesModal } from "./PermissoesModal";

type Aba = "papeis" | "usuarios";

/** TL-09/TL-10 — Admin de Papéis/Permissões/Usuários (RF-013/015/016, TASK-07.5). */
export function PapeisClient({
  projeto,
  papeis,
  usuarios,
  usuarioAutenticadoId,
}: {
  projeto: ProjetoResponse;
  papeis: PapelResponse[];
  usuarios: UsuarioProjetoResponse[];
  usuarioAutenticadoId: string | null;
}) {
  const router = useRouter();
  const [aba, setAba] = useState<Aba>("papeis");
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);

  const [papelModal, setPapelModal] = useState<{ papel: PapelResponse | null } | null>(null);
  const [permissoesModal, setPermissoesModal] = useState<PapelResponse | null>(null);
  const [associarModal, setAssociarModal] = useState(false);
  const [excluirPapel, setExcluirPapel] = useState<PapelResponse | null>(null);
  const [removerUsuario, setRemoverUsuario] = useState<UsuarioProjetoResponse | null>(null);

  const papeisOrdenados = ordenarPapeisPorNome(papeis);
  const usuariosOrdenados = ordenarUsuariosPorNome(usuarios);
  const elegiveis = papeisAssociaveis(papeis);

  function tratarErro(mensagem: string) {
    setErro(mensagem);
    setSucesso(null);
  }

  function tratarSucesso(mensagem: string) {
    setSucesso(mensagem);
    setErro(null);
    router.refresh();
  }

  async function confirmarExclusaoPapel() {
    if (!excluirPapel) return;
    const res = await fetch(`/api/papeis/${excluirPapel.id}`, { method: "DELETE" });
    setExcluirPapel(null);
    if (!res.ok) {
      tratarErro(mensagemErroPapeis(res.status, `Não foi possível excluir "${excluirPapel.nome}".`));
      return;
    }
    tratarSucesso(`Papel "${excluirPapel.nome}" excluído com sucesso.`);
  }

  async function confirmarRemocaoUsuario() {
    if (!removerUsuario) return;
    const res = await fetch(`/api/projetos/${projeto.id}/usuarios/${removerUsuario.usuarioId}`, {
      method: "DELETE",
    });
    setRemoverUsuario(null);
    if (!res.ok) {
      tratarErro(mensagemErroPapeis(res.status, `Não foi possível remover "${removerUsuario.nome}".`));
      return;
    }
    tratarSucesso(`"${removerUsuario.nome}" removido do projeto.`);
  }

  return (
    <>
      <div className="page-header">
        <h1>Papéis, permissões e usuários</h1>
      </div>

      {erro && (
        <div className="toast toast-error" role="alert" style={{ marginBottom: "16px" }}>
          <span style={{ flex: 1 }}>{erro}</span>
          <button className="btn btn-text" type="button" onClick={() => setErro(null)} aria-label="Fechar erro">
            ✕
          </button>
        </div>
      )}
      {sucesso && (
        <div className="toast toast-success" role="status" aria-live="polite" style={{ marginBottom: "16px" }}>
          {sucesso}
        </div>
      )}

      <div className="tabs" role="tablist" aria-label="Papéis, permissões e usuários">
        <button role="tab" aria-selected={aba === "papeis"} type="button" onClick={() => setAba("papeis")}>
          Papéis
        </button>
        <button role="tab" aria-selected={aba === "usuarios"} type="button" onClick={() => setAba("usuarios")}>
          Usuários
        </button>
      </div>

      {aba === "papeis" && (
        <section aria-label="Papéis do projeto">
          <table>
            <thead>
              <tr>
                <th>Papel</th>
                <th>Chave</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {papeisOrdenados.map((p) => (
                <tr key={p.id}>
                  <td>
                    {p.nome} {p.protegido && <span className="text-secondary">(protegido)</span>}
                  </td>
                  <td>{p.chave}</td>
                  <td style={{ display: "flex", gap: "8px" }}>
                    <button className="btn btn-text" type="button" onClick={() => setPermissoesModal(p)}>
                      Permissões
                    </button>
                    <button
                      className="btn btn-text"
                      type="button"
                      disabled={p.protegido}
                      onClick={() => setPapelModal({ papel: p })}
                    >
                      Editar
                    </button>
                    <button
                      className="btn btn-text"
                      type="button"
                      disabled={p.protegido}
                      onClick={() => setExcluirPapel(p)}
                    >
                      Excluir
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <button
            className="btn btn-outline"
            type="button"
            style={{ marginTop: "var(--space-md)" }}
            onClick={() => setPapelModal({ papel: null })}
          >
            + Novo papel
          </button>
        </section>
      )}

      {aba === "usuarios" && (
        <section aria-label="Usuários do projeto">
          <table>
            <thead>
              <tr>
                <th>Nome</th>
                <th>E-mail</th>
                <th>Papéis</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {usuariosOrdenados.length === 0 && (
                <tr>
                  <td colSpan={4} className="text-secondary">
                    Nenhum usuário associado a este projeto.
                  </td>
                </tr>
              )}
              {usuariosOrdenados.map((u) => (
                <tr key={u.usuarioId}>
                  <td>{u.nome}</td>
                  <td>{u.email}</td>
                  <td>{u.papeis.join(", ")}</td>
                  <td>
                    <button className="btn btn-text" type="button" onClick={() => setRemoverUsuario(u)}>
                      Remover
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <button
            className="btn btn-outline"
            type="button"
            style={{ marginTop: "var(--space-md)" }}
            disabled={elegiveis.length === 0}
            onClick={() => setAssociarModal(true)}
          >
            + Associar usuário
          </button>
          {elegiveis.length === 0 && (
            <p className="text-secondary">Crie ao menos um papel não protegido antes de associar usuários.</p>
          )}
        </section>
      )}

      {papelModal && (
        <PapelModal
          projetoId={projeto.id}
          papel={papelModal.papel}
          onFechar={() => setPapelModal(null)}
          onSalvo={() => {
            setPapelModal(null);
            tratarSucesso("Papel salvo com sucesso.");
          }}
          onErro={(msg) => {
            setPapelModal(null);
            tratarErro(msg);
          }}
        />
      )}

      {permissoesModal && (
        <PermissoesModal
          papel={permissoesModal}
          bloqueadoPorAutoconcessao={autorPossuiPapel(usuarios, usuarioAutenticadoId, permissoesModal.chave)}
          onFechar={() => {
            setPermissoesModal(null);
            router.refresh();
          }}
          onErro={tratarErro}
        />
      )}

      {associarModal && (
        <AssociarUsuarioModal
          projetoId={projeto.id}
          papeisAssociaveis={elegiveis}
          onFechar={() => setAssociarModal(false)}
          onSalvo={() => {
            setAssociarModal(false);
            tratarSucesso("Usuário associado com sucesso.");
          }}
          onErro={(msg) => {
            setAssociarModal(false);
            tratarErro(msg);
          }}
        />
      )}

      {excluirPapel && (
        <ConfirmModal
          titulo="Excluir papel"
          mensagem={`Tem certeza de que deseja excluir "${excluirPapel.nome}"? Bloqueada se houver usuários vinculados a este papel.`}
          onCancelar={() => setExcluirPapel(null)}
          onConfirmar={confirmarExclusaoPapel}
        />
      )}

      {removerUsuario && (
        <ConfirmModal
          titulo="Remover usuário"
          mensagem={`Tem certeza de que deseja remover "${removerUsuario.nome}" deste projeto? Todos os papéis dele aqui serão removidos.`}
          onCancelar={() => setRemoverUsuario(null)}
          onConfirmar={confirmarRemocaoUsuario}
        />
      )}
    </>
  );
}
