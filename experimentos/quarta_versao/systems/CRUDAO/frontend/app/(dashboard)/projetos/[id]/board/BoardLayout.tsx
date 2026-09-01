"use client";

import { useState } from "react";
import type { BoardResponse, BoardEtapa, BoardTarefa } from "@/lib/types";
import Card from "@/components/Card";

interface BoardLayoutProps {
  board: BoardResponse;
  projetoId: string;
  onMover: (tarefaId: string, etapaDestinoId: string) => Promise<void>;
  onNovoCard: () => void;
}

/** TL-03 — Board: uma `.swimlane` por raia, `.column` por etapa (docs/design .../tl-03-board-compacto.html). */
export default function BoardLayout({ board, projetoId, onMover, onNovoCard }: BoardLayoutProps) {
  const [arrastando, setArrastando] = useState<{ tarefaId: string; etapaOrigemId: string } | null>(
    null
  );
  const [colunaSobre, setColunaSobre] = useState<string | null>(null);

  const tarefasDe = (etapaId: string, raiaId: string): BoardTarefa[] =>
    board.tarefas.filter((t) => t.etapaAtualId === etapaId && t.raiaId === raiaId);

  const destinoPermitido = (etapa: BoardEtapa): boolean => {
    if (!arrastando) return false;
    if (arrastando.etapaOrigemId === etapa.id) return false;
    const origem = board.etapas.find((e) => e.id === arrastando.etapaOrigemId);
    return !!origem?.transicoesSaida.includes(etapa.id);
  };

  const handleDrop = async (etapa: BoardEtapa) => {
    setColunaSobre(null);
    if (arrastando && destinoPermitido(etapa)) {
      await onMover(arrastando.tarefaId, etapa.id);
    }
    setArrastando(null);
  };

  return (
    <div>
      {board.raias.map((raia) => (
        <section key={raia.id} aria-label={`Board — raia ${raia.nome}`} className="swimlane">
          <div className="swimlane__title">Raia: {raia.nome}</div>
          <div className="board">
            {board.etapas.map((etapa) => {
              const tarefas = tarefasDe(etapa.id, raia.id);
              const arrastandoAlgo = !!arrastando;
              const valido = arrastandoAlgo && destinoPermitido(etapa);
              const sobre = colunaSobre === `${etapa.id}:${raia.id}`;
              const classes = [
                "column",
                sobre && valido ? "column--drop-valid" : "",
                sobre && !valido && arrastandoAlgo && arrastando?.etapaOrigemId !== etapa.id
                  ? "column--drop-invalid"
                  : "",
              ]
                .filter(Boolean)
                .join(" ");

              return (
                <div
                  key={etapa.id}
                  className={classes}
                  aria-label={`Coluna ${etapa.nome}`}
                  onDragOver={(e) => {
                    if (!arrastando) return;
                    e.preventDefault();
                    setColunaSobre(`${etapa.id}:${raia.id}`);
                  }}
                  onDragLeave={() => setColunaSobre((c) => (c === `${etapa.id}:${raia.id}` ? null : c))}
                  onDrop={(e) => {
                    e.preventDefault();
                    handleDrop(etapa);
                  }}
                >
                  <div className="column__header">
                    <span>{etapa.nome}</span>
                    <span className="badge badge-neutro-contador">{tarefas.length}</span>
                  </div>

                  {tarefas.length === 0 ? (
                    <p className="text-secondary" style={{ textAlign: "center", padding: "var(--space-md) 0" }}>
                      Sem tarefas nesta etapa
                    </p>
                  ) : (
                    tarefas.map((tarefa) => (
                      <Card
                        key={tarefa.id}
                        tarefa={tarefa}
                        projetoId={projetoId}
                        arrastavel
                        onDragStart={() => setArrastando({ tarefaId: tarefa.id, etapaOrigemId: etapa.id })}
                        onDragEnd={() => {
                          setArrastando(null);
                          setColunaSobre(null);
                        }}
                      />
                    ))
                  )}

                  <button type="button" className="btn btn-text" onClick={onNovoCard}>
                    + Novo card
                  </button>
                </div>
              );
            })}
          </div>
        </section>
      ))}
    </div>
  );
}
