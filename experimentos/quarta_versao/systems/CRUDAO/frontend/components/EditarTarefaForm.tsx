"use client";

import { useState } from "react";
import type { TarefaDetalhe } from "@/lib/types";

interface EditarTarefaFormProps {
  tarefa: TarefaDetalhe;
  onSalvar: (dados: { titulo: string; descricaoEscopo?: string }) => Promise<void>;
  loading?: boolean;
}

export default function EditarTarefaForm({
  tarefa,
  onSalvar,
  loading = false,
}: EditarTarefaFormProps) {
  const [titulo, setTitulo] = useState(tarefa.titulo);
  const [descricao, setDescricao] = useState(tarefa.descricaoEscopo || "");
  const [erro, setErro] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErro(null);

    if (!titulo.trim()) {
      setErro("Título é obrigatório");
      return;
    }

    try {
      await onSalvar({
        titulo: titulo.trim(),
        descricaoEscopo: descricao.trim() || undefined,
      });
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Erro ao salvar";
      setErro(msg);
    }
  };

  // Congelamento: se tarefa foi iniciada, campos estruturais desabilitados
  const desabilitado = tarefa.iniciada;

  return (
    <form onSubmit={handleSubmit} className="bg-white rounded-lg border border-gray-200 p-4 space-y-4">
      <h3 className="text-sm font-semibold text-gray-900">
        Editar Tarefa
      </h3>

      {desabilitado && (
        <div className="p-3 bg-yellow-50 border border-yellow-200 rounded text-sm text-yellow-700">
          ⚠️ Tarefa iniciada — campos estruturais estão bloqueados.
        </div>
      )}

      {erro && (
        <div className="p-3 bg-red-50 border border-red-200 rounded text-sm text-red-700">
          {erro}
        </div>
      )}

      {/* Título */}
      <div>
        <label htmlFor="titulo" className="block text-sm font-medium text-gray-700 mb-1">
          Título
        </label>
        <input
          id="titulo"
          type="text"
          value={titulo}
          onChange={(e) => setTitulo(e.target.value)}
          disabled={desabilitado || loading}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg text-gray-900 disabled:bg-gray-100 disabled:cursor-not-allowed"
        />
      </div>

      {/* Descrição */}
      <div>
        <label htmlFor="descricao" className="block text-sm font-medium text-gray-700 mb-1">
          Descrição/Escopo
        </label>
        <textarea
          id="descricao"
          value={descricao}
          onChange={(e) => setDescricao(e.target.value)}
          disabled={loading}
          rows={4}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg text-gray-900 disabled:bg-gray-100 disabled:cursor-not-allowed"
        />
      </div>

      {/* Info campos estruturais */}
      {desabilitado && (
        <div className="p-3 bg-blue-50 border border-blue-200 rounded text-xs text-blue-700">
          <p className="font-medium mb-1">Campos estruturais bloqueados:</p>
          <ul className="list-disc list-inside space-y-1">
            <li>Etapa, Raia, Responsável (bloqueados após início)</li>
          </ul>
        </div>
      )}

      {/* Botão de salvar */}
      <button
        type="submit"
        disabled={loading}
        className="w-full px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition font-medium"
      >
        {loading ? "Salvando..." : "Salvar alterações"}
      </button>
    </form>
  );
}
