"use client";

import { useState } from "react";
import type { Raia } from "@/lib/types";

interface RaiasListProps {
  projetoId: string;
  raias: Raia[];
  onRefresh: () => void;
}

/** TL-08 — aba "Raias". */
export default function RaiasList({ projetoId, raias, onRefresh }: RaiasListProps) {
  const [novoNome, setNovoNome] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);

  const handleCriar = async () => {
    setOcupado(true);
    setErro(null);
    try {
      const res = await fetch("/api/admin/raias", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ projetoId, nome: novoNome.trim(), ordem: raias.length + 1 }),
      });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || `Erro ${res.status}`);
      }
      setNovoNome("");
      onRefresh();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao criar raia");
    } finally {
      setOcupado(false);
    }
  };

  const handleExcluir = async (raia: Raia) => {
    if (raia.global) return;
    if (!confirm(`Excluir a raia "${raia.nome}"?`)) return;
    setOcupado(true);
    setErro(null);
    try {
      const res = await fetch(`/api/admin/raias/${raia.id}`, { method: "DELETE" });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || "Não é possível excluir: há tarefas ativas vinculadas (RN-005).");
      }
      onRefresh();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao excluir raia");
    } finally {
      setOcupado(false);
    }
  };

  return (
    <div>
      {erro && (
        <div className="toast toast-error" role="alert" style={{ marginBottom: "var(--space-md)" }}>
          {erro}
        </div>
      )}

      {raias.length === 0 ? (
        <div className="empty-state">Nenhuma raia criada.</div>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Ordem</th>
              <th>Raia</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {raias.map((raia) => (
              <tr key={raia.id}>
                <td>{raia.ordem}</td>
                <td>
                  {raia.nome}
                  {raia.global ? " (global)" : ""}
                </td>
                <td>
                  <button
                    type="button"
                    className="btn btn-text"
                    onClick={() => handleExcluir(raia)}
                    disabled={raia.global || ocupado}
                    title={raia.global ? "Raia global não pode ser excluída (RN-CB-005)" : undefined}
                  >
                    Excluir
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <div className="row" style={{ marginTop: "var(--space-md)" }}>
        <input
          placeholder="Nome da raia"
          value={novoNome}
          onChange={(e) => setNovoNome(e.target.value)}
          disabled={ocupado}
        />
        <button type="button" className="btn btn-outline" onClick={handleCriar} disabled={ocupado || !novoNome.trim()}>
          {ocupado ? "Salvando…" : "+ Nova raia"}
        </button>
      </div>
    </div>
  );
}
