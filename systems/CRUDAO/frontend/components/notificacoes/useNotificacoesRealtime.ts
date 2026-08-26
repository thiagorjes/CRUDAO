"use client";

import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { NotificacaoResponse } from "@/lib/notificacoes";

/**
 * Assina `/topic/notificacoes/{usuarioId}` (RF-005, ADR-004). Diferente do board (TASK-07.2), o
 * payload aqui já é a notificação completa (mensagem enxuta, dashboard-notificacoes.md), então cada
 * evento é aplicado direto via `onNotificacao` — sem precisar de resync via REST. A subscrição do
 * tópico já é autorizada pelo backend (`ChannelInterceptor` só aceita `usuarioId` == principal do
 * JWT da sessão WS), mas o guia técnico da task pede reforço decorativo client-side: eventos com
 * `usuarioId` diferente do da sessão atual são descartados antes de chegar no callback.
 *
 * Mesmo padrão de ticket de curta duração + backoff de reconexão (1s→30s) de `useBoardRealtime`.
 */
export function useNotificacoesRealtime(
  usuarioId: string,
  backendPublicUrl: string,
  onNotificacao: (notificacao: NotificacaoResponse) => void,
) {
  const clientRef = useRef<Client | null>(null);
  const tentativaRef = useRef(0);
  const encerradoRef = useRef(false);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const onNotificacaoRef = useRef(onNotificacao);
  onNotificacaoRef.current = onNotificacao;

  useEffect(() => {
    encerradoRef.current = false;
    conectar();

    return () => {
      encerradoRef.current = true;
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      clientRef.current?.deactivate();
    };

    async function conectar() {
      if (encerradoRef.current) return;

      let ticket: string;
      try {
        const res = await fetch("/api/ws-ticket", { method: "POST" });
        if (!res.ok) throw new Error("falha ao obter ticket de conexão");
        const dados = await res.json();
        ticket = dados.ticket;
      } catch {
        agendarReconexao();
        return;
      }

      if (encerradoRef.current) return;

      const client = new Client({
        webSocketFactory: () => new SockJS(`${backendPublicUrl}/ws?ticket=${ticket}`),
        reconnectDelay: 0,
        onConnect: () => {
          tentativaRef.current = 0;
          client.subscribe(`/topic/notificacoes/${usuarioId}`, (frame) => {
            const notificacao = JSON.parse(frame.body) as NotificacaoResponse & {
              usuarioId?: string;
            };
            if (notificacao.usuarioId && notificacao.usuarioId !== usuarioId) return;
            onNotificacaoRef.current(notificacao);
          });
        },
        onWebSocketClose: () => {
          if (!encerradoRef.current) agendarReconexao();
        },
        onStompError: () => {
          if (!encerradoRef.current) agendarReconexao();
        },
      });

      clientRef.current = client;
      client.activate();
    }

    function agendarReconexao() {
      if (encerradoRef.current) return;
      const tentativa = tentativaRef.current++;
      const atraso = Math.min(1000 * 2 ** tentativa, 30_000);
      timeoutRef.current = setTimeout(conectar, atraso);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [usuarioId, backendPublicUrl]);
}
