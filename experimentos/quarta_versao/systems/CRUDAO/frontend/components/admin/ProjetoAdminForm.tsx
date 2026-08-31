"use client";

import { useState } from "react";
import type { ProjtoDetalhe } from "@/lib/types";

interface ProjetoAdminFormProps {
  projeto: ProjtoDetalhe;
  projetoId: string;
}

export default function ProjetoAdminForm({ projeto, projetoId }: ProjetoAdminFormProps) {
  const [nome, setNome] = useState(projeto.nome);
  const [descricao, setDescricao] = useState(projeto.descricao || "");
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  const handleSalvar = async () => {
    setSalvando(true);
    setErro(null);
    setSucesso(null);

    try {
      const res = await fetch(`/api/admin/projeto/${projetoId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nome, descricao }),
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || `Erro ${res.status}`);
      }

      setSucesso("Projeto salvo com sucesso!");
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Erro ao salvar";
      setErro(msg);
    } finally {
      setSalvando(false);
    }
  };

  const handleFinalizar = async () => {
    setSalvando(true);
    setErro(null);

    try {
      const res = await fetch(`/api/admin/projeto/${projetoId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ...projeto, finalizado: true }),
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || `Erro ${res.status}`);
      }

      setSucesso("Projeto finalizado!");
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Erro ao finalizar";
      setErro(msg);
    } finally {
      setSalvando(false);
    }
  };

  return (
    <div className="space-y-4">
      {erro && <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded text-sm">{erro}</div>}
      {sucesso && <div className="p-3 bg-green-50 border border-green-200 text-green-700 rounded text-sm">{sucesso}</div>}

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Nome</label>
        <input
          type="text"
          value={nome}
          onChange={(e) => setNome(e.target.value)}
          disabled={salvando || projeto.finalizado}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg disabled:bg-gray-100 disabled:cursor-not-allowed"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Descrição</label>
        <textarea
          value={descricao}
          onChange={(e) => setDescricao(e.target.value)}
          disabled={salvando || projeto.finalizado}
          rows={4}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg disabled:bg-gray-100 disabled:cursor-not-allowed"
        />
      </div>

      {projeto.finalizado && (
        <div className="p-3 bg-yellow-50 border border-yellow-200 text-yellow-700 rounded text-sm">
          ⚠️ Projeto finalizado — apenas leitura.
        </div>
      )}

      <div className="flex gap-3">
        <button
          onClick={handleSalvar}
          disabled={salvando || projeto.finalizado}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
        >
          {salvando ? "Salvando..." : "Salvar"}
        </button>

        {!projeto.finalizado && (
          <button
            onClick={handleFinalizar}
            disabled={salvando}
            className="px-4 py-2 bg-orange-600 text-white rounded-lg hover:bg-orange-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            Finalizar Projeto
          </button>
        )}
      </div>
    </div>
  );
}
