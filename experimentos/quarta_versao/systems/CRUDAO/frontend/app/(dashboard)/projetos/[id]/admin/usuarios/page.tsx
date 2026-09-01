"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import type { UsuarioProjetoPapel, Papel, UsuarioBusca } from "@/lib/types";

/** TL-10 — Usuários do Projeto (associar / remover / trocar papel). */
export default function UsuariosAdminPage() {
  const params = useParams();
  const projetoId = params.id as string;

  const [vinculos, setVinculos] = useState<UsuarioProjetoPapel[]>([]);
  const [papeis, setPapeis] = useState<Papel[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);

  // associação
  const [busca, setBusca] = useState("");
  const [resultados, setResultados] = useState<UsuarioBusca[]>([]);
  const [selecionado, setSelecionado] = useState<UsuarioBusca | null>(null);
  const [papelNovo, setPapelNovo] = useState("");

  const carregar = useCallback(async () => {
    try {
      setLoading(true);
      const [rv, rp] = await Promise.all([
        fetch(`/api/projetos/${projetoId}/usuarios`),
        fetch(`/api/admin/papeis?projetoId=${projetoId}`),
      ]);
      if (!rv.ok) throw new Error((await rv.json()).error || "Erro ao carregar usuários");
      setVinculos(await rv.json());
      setPapeis(rp.ok ? await rp.json() : []);
      setErro(null);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar usuários do projeto");
    } finally {
      setLoading(false);
    }
  }, [projetoId]);

  useEffect(() => {
    carregar();
  }, [carregar]);

  // autocomplete
  useEffect(() => {
    if (busca.trim().length < 3) {
      setResultados([]);
      return;
    }
    const t = setTimeout(async () => {
      try {
        const res = await fetch(
          `/api/projetos/${projetoId}/usuarios/buscar?q=${encodeURIComponent(busca.trim())}`
        );
        setResultados(res.ok ? await res.json() : []);
      } catch {
        setResultados([]);
      }
    }, 250);
    return () => clearTimeout(t);
  }, [busca, projetoId]);

  const associar = async (usuarioId: string, papelId: string) => {
    setErro(null);
    setOcupado(true);
    try {
      const res = await fetch(`/api/projetos/${projetoId}/usuarios`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ usuarioId, papelId }),
      });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || `Erro ${res.status}`);
      }
      setBusca("");
      setSelecionado(null);
      setPapelNovo("");
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao associar usuário");
    } finally {
      setOcupado(false);
    }
  };

  const remover = async (usuarioId: string) => {
    if (!confirm("Remover este usuário do projeto?")) return;
    setErro(null);
    setOcupado(true);
    try {
      const res = await fetch(`/api/projetos/${projetoId}/usuarios/${usuarioId}`, { method: "DELETE" });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || `Erro ${res.status}`);
      }
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao remover usuário");
    } finally {
      setOcupado(false);
    }
  };

  const trocarPapel = async (usuarioId: string, papelId: string) => {
    // Backend só tem associar/remover — troca = remove todos os vínculos e associa o novo papel.
    setErro(null);
    setOcupado(true);
    try {
      const rd = await fetch(`/api/projetos/${projetoId}/usuarios/${usuarioId}`, { method: "DELETE" });
      if (!rd.ok) throw new Error((await rd.json()).message || "Erro ao trocar papel");
      const ra = await fetch(`/api/projetos/${projetoId}/usuarios`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ usuarioId, papelId }),
      });
      if (!ra.ok) throw new Error((await ra.json()).message || "Erro ao trocar papel");
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao trocar papel");
    } finally {
      setOcupado(false);
    }
  };

  if (loading) return <div className="skeleton" style={{ height: 16, width: "80%" }} />;

  return (
    <section aria-label="Usuários associados">
      {erro && (
        <div className="toast toast-error" role="alert" style={{ marginBottom: "var(--space-md)" }}>
          {erro}
        </div>
      )}

      {vinculos.length === 0 ? (
        <div className="empty-state">Nenhum usuário associado a este projeto ainda.</div>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Usuário</th>
              <th>Papel</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {vinculos.map((v) => (
              <tr key={`${v.usuarioId}:${v.papelId}`}>
                <td>{v.usuarioNome}</td>
                <td>
                  <select
                    value={v.papelId}
                    disabled={ocupado}
                    onChange={(e) => trocarPapel(v.usuarioId, e.target.value)}
                    aria-label={`Papel de ${v.usuarioNome}`}
                  >
                    {papeis.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.nome}
                      </option>
                    ))}
                  </select>
                </td>
                <td>
                  <button type="button" className="btn btn-text" onClick={() => remover(v.usuarioId)} disabled={ocupado}>
                    Remover
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h2 style={{ fontSize: 14, marginTop: "var(--space-lg)" }}>Associar usuário</h2>
      <div className="stack" style={{ maxWidth: 480 }}>
        <div className="form-field">
          <label htmlFor="busca-usuario">Buscar por nome ou e-mail (mín. 3 caracteres)</label>
          <input
            id="busca-usuario"
            value={selecionado ? selecionado.nome : busca}
            onChange={(e) => {
              setSelecionado(null);
              setBusca(e.target.value);
            }}
            disabled={ocupado}
          />
          {!selecionado && resultados.length > 0 && (
            <ul style={{ listStyle: "none", margin: 0, padding: 0, border: "1px solid var(--color-border)", borderRadius: "var(--radius-button)" }}>
              {resultados.map((u) => (
                <li key={u.id}>
                  <button
                    type="button"
                    className="btn btn-text"
                    style={{ width: "100%", justifyContent: "flex-start" }}
                    onClick={() => {
                      setSelecionado(u);
                      setResultados([]);
                    }}
                  >
                    {u.nome} <span className="text-secondary">· {u.email}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="form-field">
          <label htmlFor="papel-novo">Papel</label>
          <select id="papel-novo" value={papelNovo} onChange={(e) => setPapelNovo(e.target.value)} disabled={ocupado}>
            <option value="">Selecione…</option>
            {papeis.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nome}
              </option>
            ))}
          </select>
        </div>

        <button
          type="button"
          className="btn btn-primary"
          disabled={ocupado || !selecionado || !papelNovo}
          onClick={() => selecionado && associar(selecionado.id, papelNovo)}
        >
          + Associar usuário
        </button>
      </div>
    </section>
  );
}
