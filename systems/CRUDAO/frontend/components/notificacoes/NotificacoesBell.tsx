"use client";

import { useEffect, useState } from "react";
import { NotificacaoResponse } from "@/lib/notificacoes";
import { mesclarNotificacao, ordenarPorDataDesc } from "@/lib/notificacoes-logic";
import { useNotificacoesRealtime } from "./useNotificacoesRealtime";

/** Sino de notificações da topbar (RF-005, TASK-07.7) — lista não lidas + marcar como lida. */
export function NotificacoesBell({
  usuarioId,
  backendPublicUrl,
}: {
  usuarioId: string;
  backendPublicUrl: string;
}) {
  const [notificacoes, setNotificacoes] = useState<NotificacaoResponse[]>([]);
  const [aberto, setAberto] = useState(false);

  useEffect(() => {
    let cancelado = false;
    fetch("/api/notificacoes?apenasNaoLidas=true")
      .then((res) => (res.ok ? res.json() : []))
      .then((dados: NotificacaoResponse[]) => {
        if (!cancelado) setNotificacoes(ordenarPorDataDesc(dados));
      })
      .catch(() => {
        /* melhor esforço — o sino simplesmente fica vazio até a próxima notificação em tempo real */
      });
    return () => {
      cancelado = true;
    };
  }, []);

  useNotificacoesRealtime(usuarioId, backendPublicUrl, (notificacao) => {
    setNotificacoes((atuais) => mesclarNotificacao(atuais, notificacao));
  });

  async function marcarComoLida(id: string) {
    const anteriores = notificacoes;
    setNotificacoes((atuais) => atuais.filter((n) => n.id !== id));

    const res = await fetch(`/api/notificacoes/${id}/lida`, { method: "POST" });
    if (!res.ok) {
      setNotificacoes(anteriores);
    }
  }

  const naoLidas = notificacoes.filter((n) => !n.lida);

  return (
    <div className="topbar__notif-wrapper">
      <button
        type="button"
        className="topbar__notif"
        aria-label="Notificações"
        onClick={() => setAberto((v) => !v)}
      >
        🔔
        {naoLidas.length > 0 && (
          <span className="topbar__notif-badge">{naoLidas.length}</span>
        )}
      </button>

      {aberto && (
        <div className="topbar__notif-painel" role="dialog" aria-label="Lista de notificações">
          {naoLidas.length === 0 ? (
            <p className="topbar__notif-vazio">Nenhuma notificação não lida.</p>
          ) : (
            <ul className="topbar__notif-lista">
              {naoLidas.map((n) => (
                <li key={n.id} className="topbar__notif-item">
                  <span>{n.mensagem}</span>
                  <button type="button" onClick={() => marcarComoLida(n.id)}>
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
