"use client";

import { useState } from "react";
import Link from "next/link";
import type { Workflow } from "@/lib/types";

interface WorkflowsListProps {
  projetoId: string;
  workflows: Workflow[];
  onRefresh: () => void;
}

/** TL-08 — aba "Colunas": lista de workflows do projeto, cada um levando à tela de etapas/transições. */
export default function WorkflowsList({ projetoId, workflows, onRefresh }: WorkflowsListProps) {
  const [novoNome, setNovoNome] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);

  const handleCriar = async () => {
    setCriando(true);
    setErro(null);
    try {
      const res = await fetch("/api/admin/workflows", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ projetoId, nome: novoNome }),
      });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || `Erro ${res.status}`);
      }
      setNovoNome("");
      onRefresh();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao criar workflow");
    } finally {
      setCriando(false);
    }
  };

  if (workflows.length === 0) {
    return (
      <div className="empty-state">
        Este projeto ainda não possui workflow configurado.
        <br />
        {erro && (
          <p className="text-secondary" style={{ color: "var(--color-error)" }}>
            {erro}
          </p>
        )}
        <div className="row" style={{ justifyContent: "center", marginTop: "var(--space-sm)" }}>
          <input
            placeholder="Nome do workflow"
            value={novoNome}
            onChange={(e) => setNovoNome(e.target.value)}
            disabled={criando}
          />
          <button type="button" className="btn btn-primary" onClick={handleCriar} disabled={criando || !novoNome.trim()}>
            {criando ? "Criando…" : "Criar workflow"}
          </button>
        </div>
      </div>
    );
  }

  return (
    <table>
      <thead>
        <tr>
          <th>Workflow</th>
          <th>Etapas</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {workflows.map((wf) => (
          <tr key={wf.id}>
            <td>{wf.nome}</td>
            <td>{wf.etapas.length}</td>
            <td>
              <Link href={`/projetos/${projetoId}/admin/workflows/${wf.id}`} className="btn btn-text">
                Gerenciar →
              </Link>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
