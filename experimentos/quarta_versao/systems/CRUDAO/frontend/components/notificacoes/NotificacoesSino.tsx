"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { Notificacao } from "@/lib/types";
import { listarNaoLidas, marcarComoLida } from "@/lib/api/notificacoes";
import { obterWsTicket } from "@/lib/api/ws-ticket";
import { NotificacoesStomp } from "@/lib/notificacoes-stomp";

interface NotificacoesSinoProps {
  usuarioId: string;
}

const ROTULO_TIPO: Record<string, string> = {
  TRANSICAO_ETAPA: "Etapa alterada",
  IMPEDIMENTO_MARCADO: "Impedimento marcado",
  IMPEDIMENTO_DESMARCADO: "Impedimento removido",
};

function rotuloTipo(tipo: string): string {
  return ROTULO_TIPO[tipo] ?? tipo;
}

function tempoRelativo(iso: string): string {
  const ms = Date.now() - new Date(iso).getTime();
  const min = Math.floor(ms / 60000);
  if (min < 1) return "agora";
  if (min < 60) return `há ${min} min`;
  const h = Math.floor(min / 60);
  if (h < 24) return `há ${h} h`;
  return `há ${Math.floor(h / 24)} d`;
}

export default function NotificacoesSino({ usuarioId }: NotificacoesSinoProps) {
  const [notificacoes, setNotificacoes] = useState<Notificacao[]>([]);
  const [aberto, setAberto] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const wrapperRef = useRef<HTMLDivElement>(null);

  const carregar = useCallback(async () => {
    try {
      const dados = await listarNaoLidas();
      setNotificacoes(dados);
      setErro(null);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar notificações");
    }
  }, []);

  // Carga inicial
  useEffect(() => {
    carregar();
  }, [carregar]);

  // Conexão STOMP — recarrega a lista a cada evento recebido
  useEffect(() => {
    if (!usuarioId) return;

    const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8081";
    const wsBaseUrl =
      (backendUrl.startsWith("https") ? "wss" : "ws") +
      "://" +
      backendUrl.replace(/^https?:\/\//, "");

    const cliente = new NotificacoesStomp(wsBaseUrl, usuarioId, obterWsTicket, {
      onEvento: () => carregar(),
      onErro: (e) => console.error("[Notificações] STOMP:", e),
    });
    cliente.conectar();

    return () => cliente.desconectar();
  }, [usuarioId, carregar]);

  // Fechar ao clicar fora
  useEffect(() => {
    if (!aberto) return;
    const onClickFora = (e: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setAberto(false);
      }
    };
    document.addEventListener("mousedown", onClickFora);
    return () => document.removeEventListener("mousedown", onClickFora);
  }, [aberto]);

  const handleMarcarLida = async (id: string) => {
    // Otimista: remove da lista; reverte recarregando em caso de falha
    setNotificacoes((prev) => prev.filter((n) => n.id !== id));
    try {
      await marcarComoLida(id);
    } catch (e) {
      console.error("[Notificações] marcar como lida:", e);
      carregar();
    }
  };

  const total = notificacoes.length;

  return (
    <div ref={wrapperRef} className="topbar__notif-wrapper">
      <button
        className="topbar__notif"
        onClick={() => setAberto((v) => !v)}
        aria-label={`Notificações${total > 0 ? ` (${total} não lidas)` : ""}`}
        aria-expanded={aberto}
      >
        🔔
        {total > 0 && <span className="topbar__notif-badge">{total}</span>}
      </button>

      {aberto && (
        <div className="topbar__notif-painel" role="menu">
          {erro && <p className="topbar__notif-vazio">{erro}</p>}
          {!erro && total === 0 && (
            <p className="topbar__notif-vazio">Nenhuma notificação não lida</p>
          )}
          {!erro && total > 0 && (
            <ul className="topbar__notif-lista">
              {notificacoes.map((n) => (
                <li key={n.id} className="topbar__notif-item">
                  <strong>{rotuloTipo(n.tipo)}</strong>
                  <span>{n.tarefaTitulo}</span>
                  <span className="text-secondary">{tempoRelativo(n.criadoEm)}</span>
                  <button type="button" onClick={() => handleMarcarLida(n.id)}>
                    Marcar como lida
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
