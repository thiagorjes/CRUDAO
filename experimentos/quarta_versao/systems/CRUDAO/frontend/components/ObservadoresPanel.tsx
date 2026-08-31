"use client";

import { useState } from "react";

interface Observador {
  id: string;
  nome: string;
}

interface ObservadoresPanelProps {
  observadores: Observador[];
  onAdicionar: (usuarioId: string) => Promise<void>;
  onRemover: (usuarioId: string) => Promise<void>;
  usuariosDisponiveis?: Observador[];
}

export default function ObservadoresPanel({
  observadores,
  onAdicionar,
  onRemover,
  usuariosDisponiveis = [],
}: ObservadoresPanelProps) {
  const [selecionado, setSelecionado] = useState<string>("");
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const handleAdicionar = async () => {
    if (!selecionado) return;

    setCarregando(true);
    setErro(null);

    try {
      await onAdicionar(selecionado);
      setSelecionado("");
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Erro ao adicionar observador";
      setErro(msg);
    } finally {
      setCarregando(false);
    }
  };

  const handleRemover = async (usuarioId: string) => {
    setCarregando(true);
    setErro(null);

    try {
      await onRemover(usuarioId);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Erro ao remover observador";
      setErro(msg);
    } finally {
      setCarregando(false);
    }
  };

  // Usuários que já são observadores
  const observadoresIds = new Set(observadores.map((o) => o.id));

  // Usuários disponíveis para adicionar
  const disponiveis = usuariosDisponiveis.filter((u) => !observadoresIds.has(u.id));

  return (
    <div className="bg-white rounded-lg border border-gray-200 p-4 space-y-4">
      <h3 className="text-sm font-semibold text-gray-900">
        Observadores
      </h3>

      {erro && (
        <div className="p-3 bg-red-50 border border-red-200 rounded text-sm text-red-700">
          {erro}
        </div>
      )}

      {/* Lista de observadores */}
      {observadores.length === 0 ? (
        <p className="text-sm text-gray-500">Nenhum observador ainda</p>
      ) : (
        <div className="space-y-2">
          {observadores.map((observador) => (
            <div
              key={observador.id}
              className="flex items-center justify-between px-3 py-2 bg-gray-50 rounded border border-gray-200"
            >
              <span className="text-sm text-gray-700">👁️ {observador.nome}</span>
              <button
                onClick={() => handleRemover(observador.id)}
                disabled={carregando}
                className="text-xs px-2 py-1 text-red-600 hover:bg-red-50 rounded disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Remover
              </button>
            </div>
          ))}
        </div>
      )}

      <hr className="border-gray-200" />

      {/* Adicionar observador */}
      <div className="space-y-2">
        <label htmlFor="usuarios" className="block text-sm font-medium text-gray-700">
          Adicionar observador
        </label>

        <div className="flex gap-2">
          <select
            id="usuarios"
            value={selecionado}
            onChange={(e) => setSelecionado(e.target.value)}
            disabled={carregando || disponiveis.length === 0}
            className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-gray-900 text-sm disabled:bg-gray-100 disabled:cursor-not-allowed"
          >
            <option value="">
              {disponiveis.length === 0
                ? "Sem usuários disponíveis"
                : "Selecione um usuário..."}
            </option>
            {disponiveis.map((usuario) => (
              <option key={usuario.id} value={usuario.id}>
                {usuario.nome}
              </option>
            ))}
          </select>

          <button
            onClick={handleAdicionar}
            disabled={!selecionado || carregando}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition font-medium text-sm"
          >
            {carregando ? "..." : "Adicionar"}
          </button>
        </div>
      </div>
    </div>
  );
}
