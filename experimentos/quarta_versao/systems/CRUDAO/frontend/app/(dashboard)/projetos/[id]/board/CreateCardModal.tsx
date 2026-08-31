"use client";

import { useState } from "react";

interface CreateCardModalProps {
  onCriar: (titulo: string, descricao?: string) => Promise<void>;
  onFechar: () => void;
}

export default function CreateCardModal({
  onCriar,
  onFechar,
}: CreateCardModalProps) {
  const [titulo, setTitulo] = useState("");
  const [descricao, setDescricao] = useState("");
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErro(null);

    if (!titulo.trim()) {
      setErro("Título é obrigatório");
      return;
    }

    try {
      setLoading(true);
      await onCriar(titulo.trim(), descricao.trim() || undefined);
      onFechar();
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Erro ao criar card";
      setErro(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black bg-opacity-50 z-40"
        onClick={onFechar}
      />

      {/* Modal */}
      <div className="fixed inset-0 flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-lg shadow-lg max-w-md w-full">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Novo Card</h2>
          </div>

          <form onSubmit={handleSubmit} className="px-6 py-4 space-y-4">
            {erro && (
              <div className="p-3 bg-red-100 border border-red-400 text-red-700 rounded-lg text-sm">
                {erro}
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Título *
              </label>
              <input
                type="text"
                value={titulo}
                onChange={(e) => setTitulo(e.target.value)}
                disabled={loading}
                placeholder="Ex: Implementar login"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Descrição (opcional)
              </label>
              <textarea
                value={descricao}
                onChange={(e) => setDescricao(e.target.value)}
                disabled={loading}
                placeholder="Ex: Integrar com Keycloak OIDC"
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
              />
            </div>

            <div className="flex gap-3 justify-end pt-4 border-t border-gray-200">
              <button
                type="button"
                onClick={onFechar}
                disabled={loading}
                className="px-4 py-2 text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition disabled:opacity-50"
              >
                Cancelar
              </button>
              <button
                type="submit"
                disabled={loading}
                className="px-4 py-2 bg-blue-600 text-white hover:bg-blue-700 rounded-lg transition disabled:opacity-50"
              >
                {loading ? "Criando..." : "Criar"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </>
  );
}
