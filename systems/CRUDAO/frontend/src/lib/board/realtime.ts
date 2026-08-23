import { Client, IMessage } from '@stomp/stompjs';
import { EventoBoard } from '@/lib/api/types';

/**
 * URL do endpoint STOMP do backend (RF-005, RNF-001). Alcançada diretamente pelo browser — não
 * passa pelo proxy Next.js (WebSocket de longa duração) — e não exige token: `/ws/**` é
 * `permitAll()` no backend (ver SecurityConfig).
 */
function urlWebSocket(): string {
  return process.env.NEXT_PUBLIC_BACKEND_WS_URL ?? 'ws://localhost:8080/ws';
}

/**
 * Conecta ao tópico de board do projeto e entrega cada {@link EventoBoard} recebido ao callback.
 * Reconecta automaticamente (STOMP heartbeat + retry do próprio @stomp/stompjs) — atualizações de
 * outros usuários devem aparecer em até 2s (RNF-001).
 */
export function conectarBoard(
  projetoId: string,
  aoReceberEvento: (evento: EventoBoard) => void,
): () => void {
  const client = new Client({
    brokerURL: urlWebSocket(),
    reconnectDelay: 2000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  });

  client.onConnect = () => {
    client.subscribe(`/topic/projetos/${projetoId}/board`, (mensagem: IMessage) => {
      try {
        aoReceberEvento(JSON.parse(mensagem.body) as EventoBoard);
      } catch {
        // Payload inesperado — ignora em vez de derrubar a conexão.
      }
    });
  };

  client.activate();
  return () => {
    client.deactivate();
  };
}
