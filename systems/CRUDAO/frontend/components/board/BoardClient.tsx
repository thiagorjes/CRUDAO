"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { BoardResponse, MembroProjeto, TarefaBoardItemResponse } from "@/lib/board";
import {
  classeDestaque,
  etapaDeMenorOrdem,
  mensagemErro,
  tarefasDe,
  transicaoPermitida,
} from "@/lib/board-logic";
import { iniciais } from "@/lib/format";
import { useBoardRealtime } from "./useBoardRealtime";
import { NovoCardModal } from "./NovoCardModal";
import { ConfirmExcluirModal } from "./ConfirmExcluirModal";

type Arrastando = { tarefaId: string; etapaOrigemId: string };

/** TL-03 — Board (RF-001, RF-002, RF-004, RF-011, RF-018, RF-019, TASK-07.2). */
export function BoardClient({
  projetoId,
  board,
  membros,
  backendPublicUrl,
}: {
  projetoId: string;
  board: BoardResponse;
  membros: MembroProjeto[];
  backendPublicUrl: string;
}) {
  const router = useRouter();
  useBoardRealtime(projetoId, backendPublicUrl);

  const [erro, setErro] = useState<string | null>(null);
  const [modalNovoCardAberto, setModalNovoCardAberto] = useState(false);
  const [tarefaParaExcluir, setTarefaParaExcluir] = useState<TarefaBoardItemResponse | null>(null);
  const [arrastando, setArrastando] = useState<Arrastando | null>(null);

  const etapasOrdenadas = useMemo(
    () => [...board.etapas].sort((a, b) => a.ordem - b.ordem),
    [board.etapas],
  );
  const raiasOrdenadas = useMemo(
    () => [...board.raias].sort((a, b) => a.ordem - b.ordem),
    [board.raias],
  );
  const etapaPorId = useMemo(() => new Map(board.etapas.map((e) => [e.id, e])), [board.etapas]);
  const membroPorId = useMemo(() => new Map(membros.map((m) => [m.usuarioId, m])), [membros]);
  const etapaMenorOrdem = useMemo(() => etapaDeMenorOrdem(board.etapas), [board.etapas]);

  async function mover(tarefaId: string, etapaDestinoId: string) {
    const res = await fetch(`/api/tarefas/${tarefaId}/mover`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ etapaDestinoId }),
    });
    if (!res.ok) {
      setErro(mensagemErro(res.status, "Não foi possível mover o card."));
      return;
    }
    router.refresh();
  }

  async function excluir(tarefa: TarefaBoardItemResponse) {
    const res = await fetch(`/api/tarefas/${tarefa.id}`, { method: "DELETE" });
    if (!res.ok) {
      setErro(mensagemErro(res.status, "Não foi possível excluir o card."));
      setTarefaParaExcluir(null);
      return;
    }
    setTarefaParaExcluir(null);
    router.refresh();
  }

  async function alternarImpedimento(tarefa: TarefaBoardItemResponse) {
    const res = await fetch(`/api/tarefas/${tarefa.id}/impedimento`, {
      method: tarefa.impedida ? "DELETE" : "POST",
    });
    if (!res.ok) {
      setErro(mensagemErro(res.status, "Não foi possível atualizar o impedimento."));
      return;
    }
    router.refresh();
  }

  return (
    <>
      {erro && (
        <div className="toast toast-error" role="alert" style={{ marginBottom: "16px" }}>
          <span style={{ flex: 1 }}>{erro}</span>
          <button className="btn btn-text" type="button" onClick={() => setErro(null)} aria-label="Fechar erro">
            ✕
          </button>
        </div>
      )}

      <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: "16px" }}>
        <button className="btn btn-primary" type="button" onClick={() => setModalNovoCardAberto(true)}>
          + Novo card
        </button>
      </div>

      {raiasOrdenadas.length === 0 && (
        <div className="empty-state">Nenhuma raia configurada para este projeto.</div>
      )}

      {raiasOrdenadas.map((raia) => (
        <section key={raia.id} aria-label={`Raia ${raia.nome}`} className="swimlane">
          <div className="swimlane__title">Raia: {raia.nome}</div>
          <div className="board">
            {etapasOrdenadas.map((etapa) => {
              const tarefas = tarefasDe(board.tarefas, raia.id, etapa.id);
              const destaque = classeDestaque(arrastando, etapa.id, etapaPorId);

              return (
                <div
                  key={etapa.id}
                  className={`column ${destaque}`}
                  aria-label={`Coluna ${etapa.nome}`}
                  onDragOver={(e) => {
                    if (arrastando && transicaoPermitida(etapaPorId, arrastando.etapaOrigemId, etapa.id)) {
                      e.preventDefault();
                    }
                  }}
                  onDrop={(e) => {
                    e.preventDefault();
                    if (arrastando) {
                      mover(arrastando.tarefaId, etapa.id);
                    }
                    setArrastando(null);
                  }}
                >
                  <div className="column__header">
                    <span>{etapa.nome}</span>
                    <span className="badge badge-neutro-contador">{tarefas.length}</span>
                  </div>

                  {tarefas.length === 0 && (
                    <div className="empty-state" style={{ padding: "16px" }}>
                      Sem tarefas nesta etapa
                    </div>
                  )}

                  {tarefas.map((tarefa) => {
                    const responsavel = tarefa.responsavelId
                      ? membroPorId.get(tarefa.responsavelId)
                      : undefined;
                    return (
                      <div
                        key={tarefa.id}
                        className={`task-card ${tarefa.impedida ? "task-card__impedido" : ""}`}
                        draggable
                        onDragStart={() =>
                          setArrastando({ tarefaId: tarefa.id, etapaOrigemId: tarefa.etapaAtualId })
                        }
                        onDragEnd={() => setArrastando(null)}
                      >
                        <a
                          href={`/projetos/${projetoId}/tarefas/${tarefa.id}`}
                          style={{ display: "block", margin: "0 0 8px", fontSize: "14px", color: "inherit" }}
                        >
                          {tarefa.titulo}
                        </a>
                        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                          {responsavel ? (
                            <span className="avatar" style={{ width: 20, height: 20, fontSize: 10 }}>
                              {iniciais(responsavel.nome)}
                            </span>
                          ) : (
                            <span />
                          )}
                          {tarefa.impedida && <span className="badge badge-warning">Impedido</span>}
                        </div>
                        <div style={{ display: "flex", gap: "4px", marginTop: "8px" }}>
                          <button
                            className="btn btn-outline"
                            type="button"
                            onClick={() => alternarImpedimento(tarefa)}
                          >
                            {tarefa.impedida ? "Desmarcar impedimento" : "Marcar impedimento"}
                          </button>
                          <button
                            className="btn btn-text"
                            type="button"
                            aria-label="Excluir card"
                            onClick={() => setTarefaParaExcluir(tarefa)}
                          >
                            Excluir
                          </button>
                        </div>
                      </div>
                    );
                  })}

                  {etapa.id === etapaMenorOrdem?.id && (
                    <button
                      className="btn btn-text"
                      type="button"
                      onClick={() => setModalNovoCardAberto(true)}
                    >
                      + Novo card
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        </section>
      ))}

      {modalNovoCardAberto && (
        <NovoCardModal
          projetoId={projetoId}
          raias={raiasOrdenadas}
          membros={membros}
          onFechar={() => setModalNovoCardAberto(false)}
          onCriado={() => {
            setModalNovoCardAberto(false);
            router.refresh();
          }}
          onErro={setErro}
        />
      )}

      {tarefaParaExcluir && (
        <ConfirmExcluirModal
          titulo={tarefaParaExcluir.titulo}
          onCancelar={() => setTarefaParaExcluir(null)}
          onConfirmar={() => excluir(tarefaParaExcluir)}
        />
      )}
    </>
  );
}
