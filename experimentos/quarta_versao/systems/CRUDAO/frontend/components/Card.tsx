"use client";

import Link from "next/link";
import type { BoardTarefa } from "@/lib/types";

interface CardProps {
  tarefa: BoardTarefa;
  projetoId: string;
  arrastavel: boolean;
  onDragStart: () => void;
  onDragEnd: () => void;
}

/** TL-03/TL-03b — card do board (`.task-card`). Ações (mover/impedir/excluir) vivem no drawer TL-04. */
export default function Card({ tarefa, projetoId, arrastavel, onDragStart, onDragEnd }: CardProps) {
  return (
    <Link
      href={`/projetos/${projetoId}/tarefas/${tarefa.id}`}
      className={`task-card${tarefa.impedida ? " task-card__impedido" : ""}`}
      style={{ textDecoration: "none", color: "inherit", display: "block" }}
      draggable={arrastavel}
      onDragStart={(e) => {
        e.dataTransfer.setData("text/plain", tarefa.id);
        e.dataTransfer.effectAllowed = "move";
        onDragStart();
      }}
      onDragEnd={onDragEnd}
    >
      <p style={{ margin: "0 0 4px", fontSize: 14 }}>{tarefa.titulo}</p>
      <div className="row row--between">
        {tarefa.impedida ? (
          <span className="badge badge-warning">Impedido</span>
        ) : (
          <span />
        )}
        {tarefa.iniciada && <span className="badge badge-tipo">Iniciado</span>}
      </div>
    </Link>
  );
}
