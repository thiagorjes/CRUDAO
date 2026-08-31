"use client";

import type { BoardResponse, BoardTarefa } from "@/lib/types";
import Card from "@/components/Card";

interface BoardLayoutProps {
  board: BoardResponse;
  onMover: (tarefaId: string, etapaDestinoId: string) => Promise<void>;
  onExcluir: (tarefaId: string) => Promise<void>;
  onToggleImpedimento: (tarefaId: string, impedida: boolean) => Promise<void>;
  projetoFinalizado?: boolean; // I1 FIX: passar informação de projeto finalizado
}

export default function BoardLayout({
  board,
  onMover,
  onExcluir,
  onToggleImpedimento,
  projetoFinalizado = false,
}: BoardLayoutProps) {
  // Agrupar tarefas por etapa e raia
  const tarefasPorEtapaRaia = new Map<string, Map<string, BoardTarefa[]>>();

  for (const etapa of board.etapas) {
    const porRaia = new Map<string, BoardTarefa[]>();
    for (const raia of board.raias) {
      porRaia.set(raia.id, []);
    }
    tarefasPorEtapaRaia.set(etapa.id, porRaia);
  }

  for (const tarefa of board.tarefas) {
    const raiaMap = tarefasPorEtapaRaia.get(tarefa.etapaAtualId);
    if (raiaMap) {
      const tarefasRaia = raiaMap.get(tarefa.raiaId) || [];
      tarefasRaia.push(tarefa);
      raiaMap.set(tarefa.raiaId, tarefasRaia);
    }
  }

  return (
    <div className="overflow-x-auto">
      <div className="flex gap-4 pb-4 min-w-full">
        {/* Coluna de raias (cabeçalho) */}
        <div className="flex-shrink-0 w-32">
          <div className="h-12" /> {/* Espaço para cabeçalho de etapas */}
          {board.raias.map((raia) => (
            <div
              key={raia.id}
              className="h-24 px-3 py-2 text-sm font-medium text-gray-700 border-b border-gray-200"
            >
              {raia.nome}
            </div>
          ))}
        </div>

        {/* Colunas de etapas */}
        {board.etapas.map((etapa) => {
          const raiaMap = tarefasPorEtapaRaia.get(etapa.id) || new Map();
          return (
            <div
              key={etapa.id}
              className="flex-shrink-0 w-80 bg-white rounded-lg border border-gray-200 overflow-hidden"
            >
              {/* Cabeçalho da etapa */}
              <div className="px-4 py-3 bg-gray-100 border-b border-gray-200">
                <h3 className="text-sm font-semibold text-gray-900">
                  {etapa.nome}
                </h3>
                <p className="text-xs text-gray-500 mt-1">
                  {board.tarefas.filter((t) => t.etapaAtualId === etapa.id).length} card(s)
                </p>
              </div>

              {/* Linhas por raia dentro da etapa */}
              <div className="divide-y divide-gray-200">
                {board.raias.map((raia) => {
                  const tarefasRaia = raiaMap.get(raia.id) || [];
                  return (
                    <div
                      key={`${etapa.id}-${raia.id}`}
                      className="px-3 py-3 min-h-24 bg-white hover:bg-gray-50 transition"
                    >
                      <div className="space-y-2">
                        {tarefasRaia.length === 0 ? (
                          <p className="text-xs text-gray-400 text-center py-4">
                            sem cards
                          </p>
                        ) : (
                          tarefasRaia.map((tarefa: BoardTarefa) => (
                            <Card
                              key={tarefa.id}
                              tarefa={tarefa}
                              etapasDisponiveis={board.etapas}
                              transicoesSaida={
                                board.etapas.find((e) => e.id === tarefa.etapaAtualId)
                                  ?.transicoesSaida || []
                              }
                              onMover={onMover}
                              onExcluir={onExcluir}
                              onToggleImpedimento={onToggleImpedimento}
                              projetoFinalizado={projetoFinalizado} // I1 FIX: passar flag
                            />
                          ))
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
