"use client";

import { useCallback, useEffect, useState } from "react";
import type { Papel } from "@/lib/types";

interface PapeisViewProps {
  projetoId: string;
}

/** TL-09 — Papéis e Permissões (matriz permissão × papel + CRUD de papéis custom). */
export default function PapeisView({ projetoId }: PapeisViewProps) {
  const [papeis, setPapeis] = useState<Papel[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [novaChave, setNovaChave] = useState("");
  const [novoNome, setNovoNome] = useState("");
  const [ocupado, setOcupado] = useState(false);

  const carregar = useCallback(async () => {
    try {
      setLoading(true);
      const res = await fetch(`/api/admin/papeis?projetoId=${projetoId}`);
      if (!res.ok) throw new Error((await res.json()).error || "Erro ao carregar papéis");
      setPapeis(await res.json());
      setErro(null);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar papéis");
    } finally {
      setLoading(false);
    }
  }, [projetoId]);

  useEffect(() => {
    carregar();
  }, [carregar]);

  const permissoesChaves = Array.from(
    new Set(papeis.flatMap((p) => p.permissoes.map((x) => x.chave)))
  ).sort();

  const toggle = async (papel: Papel, chave: string, habilitada: boolean) => {
    setErro(null);
    setOcupado(true);
    try {
      const res = await fetch(`/api/admin/papeis/${papel.id}/permissoes/${chave}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ habilitada }),
      });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(
          res.status === 403
            ? err.message || "Sem permissão (papel protegido ou é um papel seu — RN-006/RN-017)."
            : err.message || `Erro ${res.status}`
        );
      }
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao alterar permissão");
    } finally {
      setOcupado(false);
    }
  };

  const criarPapel = async () => {
    setErro(null);
    setOcupado(true);
    try {
      const res = await fetch("/api/admin/papeis", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ projetoId, chave: novaChave.trim(), nome: novoNome.trim() }),
      });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || (res.status === 422 ? "chave 'admin' é reservada" : `Erro ${res.status}`));
      }
      setNovaChave("");
      setNovoNome("");
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao criar papel");
    } finally {
      setOcupado(false);
    }
  };

  const excluirPapel = async (papel: Papel) => {
    if (!confirm(`Excluir o papel "${papel.nome}"?`)) return;
    setErro(null);
    setOcupado(true);
    try {
      const res = await fetch(`/api/admin/papeis/${papel.id}`, { method: "DELETE" });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(
          res.status === 409
            ? "Há usuários vinculados a este papel."
            : err.message || `Erro ${res.status}`
        );
      }
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao excluir papel");
    } finally {
      setOcupado(false);
    }
  };

  if (loading) return <div className="skeleton" style={{ height: 16, width: "80%" }} />;

  return (
    <section aria-label="Permissões por papel">
      {erro && (
        <div className="toast toast-error" role="alert" style={{ marginBottom: "var(--space-md)" }}>
          {erro}
        </div>
      )}

      <div style={{ overflowX: "auto" }}>
        <table>
          <thead>
            <tr>
              <th>Permissão</th>
              {papeis.map((p) => (
                <th key={p.id}>
                  {p.nome}
                  {p.protegido ? " 🔒" : ""}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {permissoesChaves.map((chave) => (
              <tr key={chave}>
                <td>{chave}</td>
                {papeis.map((p) => {
                  const atual = p.permissoes.find((x) => x.chave === chave)?.habilitada ?? false;
                  return (
                    <td key={p.id}>
                      <input
                        type="checkbox"
                        checked={atual}
                        disabled={p.protegido || ocupado}
                        aria-label={`${p.nome} — ${chave}`}
                        onChange={(e) => toggle(p, chave, e.target.checked)}
                      />
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h2 style={{ fontSize: 14, marginTop: "var(--space-lg)" }}>Papéis</h2>
      <table>
        <tbody>
          {papeis.map((p) => (
            <tr key={p.id}>
              <td>
                {p.nome} <span className="text-secondary">({p.chave})</span>
              </td>
              <td className="text-right">
                {p.protegido ? (
                  <span className="text-secondary">protegido</span>
                ) : (
                  <button type="button" className="btn btn-text" onClick={() => excluirPapel(p)} disabled={ocupado}>
                    Excluir
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="row" style={{ marginTop: "var(--space-md)" }}>
        <input
          placeholder="chave (ex.: revisor)"
          value={novaChave}
          onChange={(e) => setNovaChave(e.target.value)}
          disabled={ocupado}
        />
        <input
          placeholder="Nome do papel"
          value={novoNome}
          onChange={(e) => setNovoNome(e.target.value)}
          disabled={ocupado}
        />
        <button
          type="button"
          className="btn btn-outline"
          onClick={criarPapel}
          disabled={ocupado || !novaChave.trim() || !novoNome.trim()}
        >
          + Novo papel
        </button>
      </div>
    </section>
  );
}
