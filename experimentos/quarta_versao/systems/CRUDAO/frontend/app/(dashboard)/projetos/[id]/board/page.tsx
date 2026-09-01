"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useParams } from "next/navigation";
import type { BoardResponse, EventoBoardMessage } from "@/lib/types";
import { carregarBoard, moverTarefa, criarTarefa } from "@/lib/api/board";
import { obterUsuariosProjeto } from "@/lib/api/tarefa";
import { obterWsTicket } from "@/lib/api/ws-ticket";
import { StompManager } from "@/lib/stomp";
import BoardLayout from "./BoardLayout";
import CreateCardModal from "./CreateCardModal";

/** TL-03 — Board (docs/design/kanban-tarefas/prototypes/tl-03-board-compacto.html). */
export default function BoardPage() {
  const params = useParams();
  const projetoId = params.id as string;

  const [board, setBoard] = useState<BoardResponse | null>(null);
  const [usuarios, setUsuarios] = useState<{ id: string; nome: string }[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [mostrarModalNovoCard, setMostrarModalNovoCard] = useState(false);
  const [erroToast, setErroToast] = useState<string | null>(null);
  const [syncing, setSyncing] = useState(false);
  const stompMgrRef = useRef<StompManager | null>(null);

  const carregar = useCallback(async () => {
    try {
      setLoading(true);
      const dados = await carregarBoard(projetoId);
      setBoard(dados);
      setErro(null);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar board");
    } finally {
      setLoading(false);
    }
  }, [projetoId]);

  useEffect(() => {
    carregar();
    obterUsuariosProjeto(projetoId)
      .then(setUsuarios)
      .catch(() => setUsuarios([]));
  }, [carregar, projetoId]);

  // Tempo real (TASK-05.1/07.2): autenticação via ticket de curta duração (TASK-07.7).
  useEffect(() => {
    if (!board) return;

    const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8081";
    const wsBaseUrl =
      (backendUrl.startsWith("https") ? "wss" : "ws") + "://" + backendUrl.replace(/^https?:\/\//, "");

    const stompMgr = new StompManager(wsBaseUrl, projetoId, obterWsTicket, {
      onMensagem: (evento: EventoBoardMessage) => {
        if (syncing) return;
        setBoard((prev) => {
          if (!prev) return prev;
          const tarefas = [...prev.tarefas];
          const idx = tarefas.findIndex((t) => t.id === (evento.tarefaId ?? evento.tarefa?.id));

          if (evento.tipo === "TAREFA_CRIADA" && evento.tarefa) {
            tarefas.push(evento.tarefa);
          } else if (evento.tipo === "TAREFA_MOVIDA" && evento.tarefa && idx >= 0) {
            tarefas[idx] = evento.tarefa;
          } else if (evento.tipo === "TAREFA_EXCLUIDA" && evento.tarefaId) {
            return { ...prev, tarefas: prev.tarefas.filter((t) => t.id !== evento.tarefaId) };
          }
          return { ...prev, tarefas };
        });
      },
      onRessinc: async () => {
        setSyncing(true);
        try {
          setBoard(await carregarBoard(projetoId));
        } catch (e) {
          setErroToast(e instanceof Error ? e.message : "Erro ao sincronizar");
        } finally {
          setSyncing(false);
        }
      },
      onErro: (e) => setErroToast(e.message),
    });

    stompMgrRef.current = stompMgr;
    stompMgr.conectar().catch(() => setErroToast("Não foi possível conectar ao servidor em tempo real"));

    return () => stompMgrRef.current?.desconectar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projetoId, board?.etapas.length]);

  const handleMover = useCallback(
    async (tarefaId: string, etapaDestinoId: string) => {
      try {
        await moverTarefa(tarefaId, etapaDestinoId);
      } catch (e) {
        setErroToast(
          e instanceof Error
            ? e.message
            : 'Transição não permitida. O card retornou à posição original.'
        );
        carregar();
      }
    },
    [carregar]
  );

  const handleCriar = useCallback(
    async (dados: { titulo: string; descricao?: string; raiaId?: string; responsavelId?: string }) => {
      await criarTarefa(projetoId, {
        titulo: dados.titulo,
        descricaoEscopo: dados.descricao,
        raiaId: dados.raiaId,
        responsavelId: dados.responsavelId,
      });
      setMostrarModalNovoCard(false);
    },
    [projetoId]
  );

  if (loading) {
    return (
      <div>
        <div className="page-header">
          <h1>Board</h1>
        </div>
        <div className="board">
          <div className="column">
            <div className="skeleton" style={{ height: 70, marginBottom: 8 }} />
            <div className="skeleton" style={{ height: 70 }} />
          </div>
        </div>
      </div>
    );
  }

  if (erro || !board) {
    return (
      <div>
        <div className="page-header">
          <h1>Board</h1>
        </div>
        <div className="toast toast-error" role="alert">
          {erro || "Erro ao carregar board"}{" "}
          <button type="button" className="btn btn-outline" onClick={carregar}>
            Tentar novamente
          </button>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="page-header">
        <h1>Board</h1>
        <button type="button" className="btn btn-primary" onClick={() => setMostrarModalNovoCard(true)}>
          + Novo card
        </button>
      </div>

      {syncing && (
        <div className="toast" style={{ background: "var(--color-tipo-badge-bg)", marginBottom: "var(--space-md)" }}>
          Sincronizando com o servidor…
        </div>
      )}

      {erroToast && (
        <div className="toast toast-error" role="alert" aria-live="assertive" style={{ marginBottom: "var(--space-md)" }}>
          {erroToast}{" "}
          <button type="button" className="btn btn-text" onClick={() => setErroToast(null)}>
            Fechar
          </button>
        </div>
      )}

      <BoardLayout board={board} projetoId={projetoId} onMover={handleMover} onNovoCard={() => setMostrarModalNovoCard(true)} />

      {mostrarModalNovoCard && (
        <CreateCardModal
          raias={board.raias}
          usuarios={usuarios}
          onCriar={handleCriar}
          onFechar={() => setMostrarModalNovoCard(false)}
        />
      )}
    </div>
  );
}
