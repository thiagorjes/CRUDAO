"use client";

import { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import type { BoardTarefa, BoardEtapa } from "@/lib/types";

interface CardProps {
  tarefa: BoardTarefa;
  etapasDisponiveis: BoardEtapa[];
  transicoesSaida: string[];
  onMover: (tarefaId: string, etapaDestinoId: string) => Promise<void>;
  onExcluir: (tarefaId: string) => Promise<void>;
  onToggleImpedimento: (tarefaId: string, impedida: boolean) => Promise<void>;
  projetoFinalizado?: boolean; // I1 FIX: passar se projeto está finalizado
}

export default function Card({
  tarefa,
  etapasDisponiveis,
  transicoesSaida,
  onMover,
  onExcluir,
  onToggleImpedimento,
  projetoFinalizado = false,
}: CardProps) {
  const params = useParams();
  const projetoId = params.id as string;

  const [showMenu, setShowMenu] = useState(false);
  const [loading, setLoading] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // I2 FIX: Fechar menu ao clicar fora
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setShowMenu(false);
      }
    };

    if (showMenu) {
      document.addEventListener("click", handleClickOutside);
      return () => document.removeEventListener("click", handleClickOutside);
    }
  }, [showMenu]);

  // I2 FIX: Fechar menu com Escape
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === "Escape") setShowMenu(false);
    };

    if (showMenu) {
      document.addEventListener("keydown", handleEscape);
      return () => document.removeEventListener("keydown", handleEscape);
    }
  }, [showMenu]);

  const etapaAtual = etapasDisponiveis.find((e) => e.id === tarefa.etapaAtualId);
  const etapasDestino = etapasDisponiveis.filter((e) =>
    transicoesSaida.includes(e.id)
  );

  const handleMover = async (etapaDestinoId: string) => {
    try {
      setLoading(true);
      await onMover(tarefa.id, etapaDestinoId);
      setShowMenu(false);
    } catch (e) {
      console.error("Erro ao mover:", e);
    } finally {
      setLoading(false);
    }
  };

  const handleExcluir = async () => {
    try {
      setLoading(true);
      await onExcluir(tarefa.id);
      setShowMenu(false);
    } catch (e) {
      console.error("Erro ao excluir:", e);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleImpedimento = async () => {
    try {
      setLoading(true);
      await onToggleImpedimento(tarefa.id, tarefa.impedida);
      setShowMenu(false);
    } catch (e) {
      console.error("Erro ao atualizar impedimento:", e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className={`p-3 rounded-lg border-2 transition cursor-move ${
        tarefa.impedida
          ? "bg-yellow-50 border-yellow-300"
          : "bg-white border-gray-200 hover:border-blue-300"
      }`}
      draggable
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex-1 min-w-0">
          <Link
            href={`/projetos/${projetoId}/tarefas/${tarefa.id}`}
            className="text-sm font-medium text-gray-900 hover:text-blue-600 truncate block"
          >
            {tarefa.titulo}
          </Link>
          <p className="text-xs text-gray-500 mt-1">
            {etapaAtual?.nome}
          </p>
        </div>

        {/* Menu de ações */}
        <div className="relative flex-shrink-0" ref={menuRef}>
          <button
            onClick={() => setShowMenu(!showMenu)}
            disabled={loading || projetoFinalizado}
            title={projetoFinalizado ? "Projeto finalizado — somente leitura" : "Ações"}
            className="p-1 hover:bg-gray-100 rounded text-gray-400 hover:text-gray-600 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            ⋮
          </button>

          {showMenu && (
            <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg z-50 py-1">
              {/* Mover para outra etapa */}
              {etapasDestino.length > 0 && (
                <div className="py-1">
                  <p className="px-4 py-2 text-xs font-semibold text-gray-600 uppercase">
                    Mover para
                  </p>
                  {etapasDestino.map((etapa) => (
                    <button
                      key={etapa.id}
                      onClick={() => handleMover(etapa.id)}
                      disabled={loading || projetoFinalizado} // I1 FIX: desabilitar se projeto finalizado
                      className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-blue-50 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {etapa.nome}
                    </button>
                  ))}
                </div>
              )}

              <hr className="my-1" />

              {/* Toggle impedimento */}
              <button
                onClick={handleToggleImpedimento}
                disabled={loading || projetoFinalizado} // I1 FIX: desabilitar se projeto finalizado
                className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-yellow-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {tarefa.impedida ? "✓ Remover impedimento" : "⚠ Marcar impedimento"}
              </button>

              {/* Excluir */}
              <button
                onClick={handleExcluir}
                disabled={loading || projetoFinalizado} // I1 FIX: desabilitar se projeto finalizado
                className="block w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Excluir
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Indicador de impedimento */}
      {tarefa.impedida && (
        <div className="mt-2 pt-2 border-t border-yellow-200">
          <p className="text-xs text-yellow-700 font-medium">⚠ Impedido</p>
        </div>
      )}

      {/* Indicador de iniciado */}
      {tarefa.iniciada && (
        <div className="mt-2">
          <span className="inline-block px-2 py-1 text-xs font-medium bg-blue-100 text-blue-700 rounded">
            Iniciado
          </span>
        </div>
      )}
    </div>
  );
}
