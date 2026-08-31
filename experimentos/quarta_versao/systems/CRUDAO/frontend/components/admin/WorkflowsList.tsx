"use client";

import { useState } from "react";
import type { Workflow } from "@/lib/types";

interface WorkflowsListProps {
  projetoId: string;
  workflows: Workflow[];
  onRefresh: () => void;
}

export default function WorkflowsList({ projetoId, workflows, onRefresh }: WorkflowsListProps) {
  const [novoNome, setNovoNome] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);

  const handleCriarWorkflow = async () => {
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
      const msg = e instanceof Error ? e.message : "Erro ao criar workflow";
      setErro(msg);
    } finally {
      setCriando(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Criar Novo Workflow</h3>
        {erro && <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded text-sm mb-4">{erro}</div>}

        <div className="flex gap-2">
          <input
            type="text"
            placeholder="Nome do workflow"
            value={novoNome}
            onChange={(e) => setNovoNome(e.target.value)}
            disabled={criando}
            className="flex-1 px-3 py-2 border border-gray-300 rounded-lg disabled:bg-gray-100"
          />
          <button
            onClick={handleCriarWorkflow}
            disabled={criando || !novoNome.trim()}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            {criando ? "Criando..." : "Criar"}
          </button>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Workflows</h3>
        {workflows.length === 0 ? (
          <p className="text-gray-600">Nenhum workflow criado</p>
        ) : (
          <div className="space-y-2">
            {workflows.map((wf) => (
              <div key={wf.id} className="flex items-center justify-between p-3 bg-gray-50 rounded border border-gray-200">
                <div>
                  <p className="font-medium text-gray-900">{wf.nome}</p>
                  <p className="text-xs text-gray-600">{wf.id}</p>
                </div>
                <a
                  href={`/projetos/${projetoId}/admin/workflows/${wf.id}`}
                  className="text-blue-600 hover:text-blue-700 text-sm font-medium"
                >
                  Gerenciar →
                </a>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
