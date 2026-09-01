"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import type { UsuarioProjetoPapel } from "@/lib/types";

/**
 * TL-10 — Usuários do Projeto.
 *
 * Leitura funciona contra o backend real (`GET /api/projetos/{id}/usuarios`, adicionado nesta
 * revisão). Associar/remover/trocar papel de usuário AINDA NÃO tem endpoint no backend — não é
 * uma questão de UI, é uma feature de RBAC que precisa ser desenhada (quem pode associar quem,
 * papel `admin` protegido, autoproteção de papel) antes de implementar; ver nota em
 * docs/checklists/kanban-tarefas-ui-fidelidade-review.md.
 */
export default function UsuariosAdminPage() {
  const params = useParams();
  const projetoId = params.id as string;

  const [vinculos, setVinculos] = useState<UsuarioProjetoPapel[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    const carregar = async () => {
      try {
        setLoading(true);
        const res = await fetch(`/api/projetos/${projetoId}/usuarios`);
        if (!res.ok) {
          const err = await res.json();
          throw new Error(err.message || "Erro ao carregar usuários do projeto");
        }
        setVinculos(await res.json());
        setErro(null);
      } catch (e) {
        setErro(e instanceof Error ? e.message : "Erro ao carregar usuários do projeto");
      } finally {
        setLoading(false);
      }
    };
    carregar();
  }, [projetoId]);

  if (loading) {
    return <div className="skeleton" style={{ height: 16, width: "80%" }} />;
  }

  if (erro) {
    return (
      <div className="toast toast-error" role="alert">
        {erro}
      </div>
    );
  }

  return (
    <section aria-label="Usuários associados">
      <div className="toast" style={{ background: "var(--color-tipo-badge-bg)", marginBottom: "var(--space-md)" }}>
        Associar, remover ou trocar o papel de um usuário ainda não está disponível — essa parte do
        RBAC depende de um novo endpoint no backend.
      </div>

      {vinculos.length === 0 ? (
        <div className="empty-state">Nenhum usuário associado a este projeto ainda.</div>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Usuário</th>
              <th>Papel</th>
            </tr>
          </thead>
          <tbody>
            {vinculos.map((v) => (
              <tr key={`${v.usuarioId}:${v.papelId}`}>
                <td>{v.usuarioNome}</td>
                <td>{v.papelNome}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
