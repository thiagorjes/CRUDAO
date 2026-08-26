"use client";

import { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

/**
 * Assina `/topic/board/{projetoId}` (ADR-004, RNF-001) e resincroniza o board via
 * `router.refresh()` — o payload do evento (`EventoBoardPayload`) é enxuto de propósito (só ids +
 * `seq`, limite de 8KB do NOTIFY), então qualquer evento já é tratado como "algo mudou, busque de
 * novo", o que também cobre gap de `seq` e reconexão sem lógica extra (critérios de aceite
 * TASK-07.2).
 *
 * Cada tentativa de conexão pede um ticket novo (`POST /api/ws-ticket`, proxy Next.js — TASK-07.2):
 * o ticket é de uso único e TTL de 20s, então não pode ser reaproveitado entre reconexões. Backoff
 * de reconexão próprio (1s→30s), mesmo padrão documentado no backend (TASK-05.1/05.3).
 */
export function useBoardRealtime(projetoId: string, backendPublicUrl: string) {
  const router = useRouter();
  const clientRef = useRef<Client | null>(null);
  const tentativaRef = useRef(0);
  const encerradoRef = useRef(false);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

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
        // reconexão própria (abaixo) — a do stompjs reaproveitaria o mesmo ticket já usado.
        reconnectDelay: 0,
        onConnect: () => {
          tentativaRef.current = 0;
          client.subscribe(`/topic/board/${projetoId}`, () => {
            router.refresh();
          });
          router.refresh();
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
  }, [projetoId, backendPublicUrl]);
}
