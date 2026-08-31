"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import { useParams } from "next/navigation";
import type { BoardResponse, BoardEtapa, BoardRaia, BoardTarefa, EventoBoardMessage } from "@/lib/types";
import { carregarBoard, moverTarefa, criarTarefa, excluirTarefa, marcarImpedimento, desmarcarImpedimento } from "@/lib/api/board";
import { StompManager } from "@/lib/stomp";
import BoardLayout from "./BoardLayout";
import CreateCardModal from "./CreateCardModal";

export default function BoardPage() {
  const params = useParams();
  const projetoId = params.id as string;

  const [board, setBoard] = useState<BoardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [erroToast, setErroToast] = useState<string | null>(null);
  const [syncing, setSyncing] = useState(false); // C2 FIX: Lock durante resincronização
  const stompMgrRef = useRef<StompManager | null>(null);

  // Carregar board inicial
  useEffect(() => {
    const carregar = async () => {
      try {
        setLoading(true);
        const dados = await carregarBoard(projetoId);
        setBoard(dados);
        setErro(null);
      } catch (e) {
        const msg = e instanceof Error ? e.message : "Erro ao carregar board";
        setErro(msg);
      } finally {
        setLoading(false);
      }
    };

    carregar();
  }, [projetoId]);

  // Conectar ao STOMP para atualização em tempo real
  useEffect(() => {
    if (!board) return;

    const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8081";
    // Converter URL HTTP para WebSocket (http://... → ws://..., https://... → wss://...)
    const wsUrl = (backendUrl.startsWith("https") ? "wss" : "ws") +
      "://" + backendUrl.replace(/^https?:\/\//, "") + "/ws";

    // C1 FIX: Obter access token da sessão (via cookie) para autenticação STOMP
    // Em production, ler do localStorage/sessionStorage ou passar via props
    const token = document.cookie
      .split("; ")
      .find((row) => row.startsWith("session="))
      ?.split("=")[1];

    const stompMgr = new StompManager(wsUrl, projetoId, token, {
      onMensagem: (evento: EventoBoardMessage) => {
        // C2 FIX: Não processar eventos durante resincronização
        if (syncing) {
          console.log("[Board] Descartando evento durante sync:", evento);
          return;
        }

        console.log("[Board] Evento STOMP:", evento);
        // Atualizar o board com a nova tarefa
        setBoard((prev) => {
          if (!prev) return prev;

          const novasTarefas = [...prev.tarefas];
          const idx = novasTarefas.findIndex((t) => t.id === evento.tarefaId || t.id === evento.tarefa?.id);

          if (evento.tipo === "TAREFA_CRIADA" && evento.tarefa) {
            novasTarefas.push(evento.tarefa);
          } else if (evento.tipo === "TAREFA_MOVIDA" && evento.tarefa) {
            if (idx >= 0) {
              novasTarefas[idx] = evento.tarefa;
            }
          } else if (evento.tipo === "TAREFA_EXCLUIDA" && evento.tarefaId) {
            return {
              ...prev,
              tarefas: prev.tarefas.filter((t) => t.id !== evento.tarefaId),
            };
          }

          return { ...prev, tarefas: novasTarefas };
        });
      },
      onRessinc: async (motivo: string) => {
        // C2 FIX: Lock durante resincronização
        setSyncing(true);
        try {
          console.log("[Board] Resincronização necessária:", motivo);
          // I3 FIX: Mostrar feedback visual (via syncing state)
          const dados = await carregarBoard(projetoId);
          setBoard(dados);
        } catch (e) {
          const msg = e instanceof Error ? e.message : "Erro ao sincronizar";
          console.error("[Board] Erro ao resincronizar:", msg);
          setErroToast(msg);
        } finally {
          setSyncing(false);
        }
      },
      onErro: (erro: Error) => {
        console.error("[Board] Erro STOMP:", erro);
        setErroToast(erro.message);
      },
    });

    stompMgrRef.current = stompMgr;

    stompMgr.conectar().catch((e) => {
      console.error("[Board] Falha ao conectar STOMP:", e);
      setErroToast("Não foi possível conectar ao servidor realtime");
    });

    return () => {
      if (stompMgrRef.current) {
        stompMgrRef.current.desconectar();
      }
    };
  }, [projetoId, board?.etapas.length]); // Reconectar se etapas mudam

  const handleMover = useCallback(
    async (tarefaId: string, etapaDestinoId: string) => {
      try {
        await moverTarefa(tarefaId, etapaDestinoId);
        // Evento STOMP atualiza o board automaticamente
      } catch (e) {
        const msg = e instanceof Error ? e.message : "Erro ao mover tarefa";
        setErroToast(msg);
        // Resincronizar em caso de erro
        carregarBoard(projetoId)
          .then((dados) => setBoard(dados))
          .catch(() => {});
      }
    },
    [projetoId]
  );

  const handleCriarTarefa = useCallback(
    async (titulo: string, descricao?: string) => {
      try {
        await criarTarefa(projetoId, { titulo, descricaoEscopo: descricao });
        setShowCreateModal(false);
        // Evento STOMP atualiza o board automaticamente
      } catch (e) {
        const msg = e instanceof Error ? e.message : "Erro ao criar tarefa";
        setErroToast(msg);
      }
    },
    [projetoId]
  );

  const handleExcluir = useCallback(
    async (tarefaId: string) => {
      if (!confirm("Tem certeza que deseja excluir este card?")) return;
      try {
        await excluirTarefa(tarefaId);
        // Evento STOMP atualiza o board automaticamente
      } catch (e) {
        const msg = e instanceof Error ? e.message : "Erro ao excluir tarefa";
        setErroToast(msg);
      }
    },
    []
  );

  const handleToggleImpedimento = useCallback(
    async (tarefaId: string, impedida: boolean) => {
      try {
        if (impedida) {
          await desmarcarImpedimento(tarefaId);
        } else {
          await marcarImpedimento(tarefaId);
        }
        // Evento STOMP atualiza o board automaticamente
      } catch (e) {
        const msg = e instanceof Error ? e.message : "Erro ao atualizar impedimento";
        setErroToast(msg);
      }
    },
    []
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-gray-600">Carregando board...</p>
      </div>
    );
  }

  if (erro || !board) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-red-600">{erro || "Erro ao carregar board"}</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full bg-gray-50 p-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Board</h1>
        <button
          onClick={() => setShowCreateModal(true)}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
        >
          Novo Card
        </button>
      </div>

      {/* I3 FIX: Feedback visual durante resincronização */}
      {syncing && (
        <div className="mb-4 p-4 bg-blue-100 border border-blue-400 text-blue-700 rounded-lg flex items-center gap-2">
          <span className="inline-block animate-spin">⟳</span>
          <span>Sincronizando com servidor...</span>
        </div>
      )}

      {erroToast && (
        <div className="mb-4 p-4 bg-red-100 border border-red-400 text-red-700 rounded-lg">
          {erroToast}
          <button
            onClick={() => setErroToast(null)}
            className="ml-2 text-sm underline"
          >
            Fechar
          </button>
        </div>
      )}

      <BoardLayout
        board={board}
        onMover={handleMover}
        onExcluir={handleExcluir}
        onToggleImpedimento={handleToggleImpedimento}
        projetoFinalizado={false} // I1 FIX: TODO — adicionar ao BoardResponse via backend (GET /board)
      />

      {showCreateModal && (
        <CreateCardModal
          onCriar={handleCriarTarefa}
          onFechar={() => setShowCreateModal(false)}
        />
      )}
    </div>
  );
}
