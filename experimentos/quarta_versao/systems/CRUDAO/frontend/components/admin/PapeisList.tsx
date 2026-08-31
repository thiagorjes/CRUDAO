"use client";

import { useState } from "react";
import type { Papel } from "@/lib/types";

interface PapeisListProps {
  projetoId: string;
  papeis: Papel[];
  onRefresh: () => void;
}

export default function PapeisList({ projetoId, papeis, onRefresh }: PapeisListProps) {
  const [novoNome, setNovoNome] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);

  const handleCriarPapel = async () => {
    setCriando(true);
    setErro(null);
    setSucesso(null);

    try {
      const res = await fetch("/api/admin/papeis", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ projetoId, nome: novoNome }),
      });

      if (!res.ok) {
        const err = await res.json();
        if (res.status === 417) {
          setErro("Você não pode alterar permissões do seu próprio papel (RN-017)");
        } else {
          throw new Error(err.message || `Erro ${res.status}`);
        }
      } else {
        setNovoNome("");
        setSucesso("Papel criado com sucesso!");
        onRefresh();
      }
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Erro ao criar papel";
      setErro(msg);
    } finally {
      setCriando(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Criar Novo Papel</h3>
        {erro && <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded text-sm mb-4">{erro}</div>}
        {sucesso && <div className="p-3 bg-green-50 border border-green-200 text-green-700 rounded text-sm mb-4">{sucesso}</div>}

        <div className="flex gap-2">
          <input
            type="text"
            placeholder="Nome do papel"
            value={novoNome}
            onChange={(e) => setNovoNome(e.target.value)}
            disabled={criando}
            className="flex-1 px-3 py-2 border border-gray-300 rounded-lg disabled:bg-gray-100"
          />
          <button
            onClick={handleCriarPapel}
            disabled={criando || !novoNome.trim()}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            {criando ? "Criando..." : "Criar"}
          </button>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Papéis do Projeto</h3>
        {papeis.length === 0 ? (
          <p className="text-gray-600">Nenhum papel criado</p>
        ) : (
          <div className="space-y-2">
            {papeis.map((papel) => (
              <div
                key={papel.id}
                className="flex items-center justify-between p-3 bg-gray-50 rounded border border-gray-200"
              >
                <div>
                  <p className="font-medium text-gray-900">{papel.nome}</p>
                  <p className="text-xs text-gray-600">{papel.protegido ? "🔒 Protegido" : "Personalizável"}</p>
                </div>
                {!papel.protegido && (
                  <button className="text-blue-600 hover:text-blue-700 text-sm font-medium">
                    Editar →
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
