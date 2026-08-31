"use client";

import { useState } from "react";
import type { Raia } from "@/lib/types";

interface RaiasListProps {
  projetoId: string;
  raias: Raia[];
  onRefresh: () => void;
}

export default function RaiasList({ projetoId, raias, onRefresh }: RaiasListProps) {
  const [novoNome, setNovoNome] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);

  const handleCriarRaia = async () => {
    setCriando(true);
    setErro(null);

    try {
      const res = await fetch("/api/admin/raias", {
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
      const msg = e instanceof Error ? e.message : "Erro ao criar raia";
      setErro(msg);
    } finally {
      setCriando(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Criar Nova Raia</h3>
        {erro && <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded text-sm mb-4">{erro}</div>}

        <div className="flex gap-2">
          <input
            type="text"
            placeholder="Nome da raia"
            value={novoNome}
            onChange={(e) => setNovoNome(e.target.value)}
            disabled={criando}
            className="flex-1 px-3 py-2 border border-gray-300 rounded-lg disabled:bg-gray-100"
          />
          <button
            onClick={handleCriarRaia}
            disabled={criando || !novoNome.trim()}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            {criando ? "Criando..." : "Criar"}
          </button>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Raias</h3>
        {raias.length === 0 ? (
          <p className="text-gray-600">Nenhuma raia criada</p>
        ) : (
          <div className="space-y-2">
            {raias.map((raia) => (
              <div key={raia.id} className="flex items-center justify-between p-3 bg-gray-50 rounded border border-gray-200">
                <div>
                  <p className="font-medium text-gray-900">{raia.nome}</p>
                  <p className="text-xs text-gray-600">{raia.id} {raia.global ? "• Global" : ""}</p>
                </div>
                <button
                  onClick={() => {
                    if (confirm("Deletar esta raia?")) {
                      // TODO: implementar delete
                    }
                  }}
                  className="text-red-600 hover:text-red-700 text-sm font-medium disabled:text-gray-400"
                  disabled={raia.global}
                >
                  Deletar
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
