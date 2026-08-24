import { Client, IMessage } from '@stomp/stompjs';
import { EventoBoard } from '@/lib/api/types';

/**
 * URL do endpoint STOMP do backend (RF-005, RNF-001). Alcançada diretamente pelo browser — não
 * passa pelo proxy Next.js (WebSocket de longa duração). O handshake HTTP (`/ws/**`) é
 * `permitAll()` no backend, mas a autenticação de fato é exigida no frame STOMP CONNECT (ver
 * `Authorization` em {@link conectarBoard} e `StompAuthChannelInterceptor` no backend — corrigido
 * no code review da TASK-05.1, que apontou o handshake sem autenticação como vulnerabilidade).
 */
function urlWebSocket(): string {
  return process.env.NEXT_PUBLIC_BACKEND_WS_URL ?? 'ws://localhost:8080/ws';
}

/** Callback avisado quando a conexão STOMP falha permanentemente (ex.: sessão expirada). */
export type FalhaConexao = (motivo: string) => void;

/**
 * Conecta ao tópico de board do projeto e entrega cada {@link EventoBoard} recebido ao callback.
 * Reconecta automaticamente (STOMP heartbeat + retry do próprio @stomp/stompjs) — atualizações de
 * outros usuários devem aparecer em até 2s (RNF-001). Descarta eventos de outro projeto (guarda
 * contra a troca de projeto pelo usuário entre o disparo do evento e a entrega).
 */
export function conectarBoard(
  projetoId: string,
  aoReceberEvento: (evento: EventoBoard) => void,
  aoFalhar?: FalhaConexao,
): () => void {
  let ativo = true;
  const client = new Client({
    reconnectDelay: 2000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    beforeConnect: async () => {
      const token = await buscarTokenParaConexao();
      client.connectHeaders = { Authorization: `Bearer ${token}` };
    },
  });
  client.webSocketFactory = () => new WebSocket(urlWebSocket());

  client.onConnect = () => {
    client.subscribe(`/topic/projetos/${projetoId}/board`, (mensagem: IMessage) => {
      try {
        const evento = JSON.parse(mensagem.body) as EventoBoard;
        if (evento.projetoId === projetoId) {
          aoReceberEvento(evento);
        }
      } catch {
        // Payload inesperado — ignora em vez de derrubar a conexão.
      }
    });
  };
  client.onStompError = (frame) => {
    if (ativo) aoFalhar?.(frame.headers.message ?? 'Erro STOMP');
  };
  client.onWebSocketError = () => {
    if (ativo) aoFalhar?.('Falha na conexão WebSocket');
  };

  client.activate();
  return () => {
    ativo = false;
    client.deactivate();
  };
}

/**
 * Conecta ao tópico de resultado de um job de dashboard (RF-007, ADR-005) e entrega o payload
 * assim que chegar via STOMP — canal dedicado por job, um único resultado esperado. Se a conexão
 * falhar (ex.: sessão expirada, WebSocket indisponível), avisa via `aoFalhar` para que o chamador
 * caia no fallback de polling em `GET .../dashboard/jobs/{jobId}` (critério de aceite da TASK-05.2).
 */
export function conectarDashboard<T>(
  projetoId: string,
  jobId: string,
  aoReceberResultado: (resultado: T) => void,
  aoFalhar?: FalhaConexao,
  aoConectar?: () => void,
): () => void {
  let ativo = true;
  const client = new Client({
    reconnectDelay: 2000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    beforeConnect: async () => {
      const token = await buscarTokenParaConexao();
      client.connectHeaders = { Authorization: `Bearer ${token}` };
    },
  });
  client.webSocketFactory = () => new WebSocket(urlWebSocket());

  client.onConnect = () => {
    client.subscribe(`/topic/projetos/${projetoId}/dashboard/${jobId}`, (mensagem: IMessage) => {
      try {
        aoReceberResultado(JSON.parse(mensagem.body) as T);
      } catch {
        // Payload inesperado — ignora.
      }
    });
    // Corrida entre o cálculo (rápido, achado de code review da TASK-05.2) e o handshake STOMP: o
    // resultado pode ter sido publicado via pg_notify antes da subscription se estabelecer. Avisa
    // o chamador para fazer uma consulta de "catch-up" ao job assim que a conexão é confirmada.
    if (ativo) aoConectar?.();
  };
  client.onStompError = (frame) => {
    if (ativo) aoFalhar?.(frame.headers.message ?? 'Erro STOMP');
  };
  client.onWebSocketError = () => {
    if (ativo) aoFalhar?.('Falha na conexão WebSocket');
  };

  client.activate();
  return () => {
    ativo = false;
    client.deactivate();
  };
}

async function buscarTokenParaConexao(): Promise<string> {
  const resposta = await fetch('/api/ws-token');
  if (!resposta.ok) {
    throw new Error('Não foi possível obter token para a conexão em tempo real.');
  }
  const dados = (await resposta.json()) as { accessToken: string };
  return dados.accessToken;
}
