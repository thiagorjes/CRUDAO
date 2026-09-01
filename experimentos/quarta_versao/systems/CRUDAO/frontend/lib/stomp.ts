/**
 * Cliente STOMP/WebSocket para atualização em tempo real do board.
 * Implementa reconexão automática e resincronização por gap de sequência (ADR-004).
 */

import type { EventoBoardMessage, BoardResponse } from "./types";
import { carregarBoard } from "./api/board";

type StompFrameHeader = Record<string, string>;

interface StompFrame {
  command: string;
  headers: StompFrameHeader;
  body: string;
}

interface StompClient {
  connect(headers: StompFrameHeader, callback: () => void): void;
  subscribe(destination: string, callback: (msg: StompMessage) => void): Subscription;
  send(destination: string, headers: StompFrameHeader, body: string): void;
  disconnect(callback?: () => void): void;
}

interface StompMessage {
  body: string;
  headers: StompFrameHeader;
}

interface Subscription {
  unsubscribe(): void;
}

/**
 * Gerenciador de conexão STOMP com reconexão + resincronização.
 * Uso:
 *   const stompMgr = new StompManager("ws://localhost:8081/ws", "projetoId", {
 *     onMensagem: (evento) => { ... },
 *     onRessinc: () => { ... },
 *   });
 *   stompMgr.conectar();
 *   // ... mais tarde
 *   stompMgr.desconectar();
 */
export class StompManager {
  private wsBaseUrl: string;
  private projetoId: string;
  private getTicket: () => Promise<string>;
  private config: {
    onMensagem?: (evento: EventoBoardMessage) => void;
    onRessinc?: (motivo: string) => void;
    onErro?: (erro: Error) => void;
  };
  private subscription?: Subscription;
  private ultimoSeq: number = 0;
  private wsConnected: boolean = false;
  private ws?: WebSocket;
  private reconectTimer?: NodeJS.Timeout;
  private tentativasReconexao: number = 0;
  private encerrado: boolean = false;

  constructor(
    wsBaseUrl: string,
    projetoId: string,
    getTicket: () => Promise<string>,
    config?: typeof this.config
  ) {
    this.wsBaseUrl = wsBaseUrl.replace(/\/$/, "");
    this.projetoId = projetoId;
    this.getTicket = getTicket;
    this.config = config ?? {};
  }

  async conectar(): Promise<void> {
    if (this.encerrado) return;
    const ticket = await this.getTicket();
    if (this.encerrado) return;
    return new Promise((resolve, reject) => {
      try {
        this.ws = new WebSocket(
          `${this.wsBaseUrl}/ws?ticket=${encodeURIComponent(ticket)}`
        );

        this.ws.onopen = () => {
          this.wsConnected = true;
          this.tentativasReconexao = 0;
          this._conectarStomp();
          resolve();
        };

        this.ws.onclose = () => {
          this.wsConnected = false;
          this._reconectar();
        };

        this.ws.onerror = (evt) => {
          const erro = new Error("Erro na conexão WebSocket");
          this.config.onErro?.(erro);
          reject(erro);
        };

        this.ws.onmessage = (evt) => {
          this._processarMensagemStomp(evt.data);
        };
      } catch (e) {
        reject(e);
      }
    });
  }

  private _conectarStomp(): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;

    // Enviar CONNECT para ativar STOMP
    const connectFrame: StompFrame = {
      command: "CONNECT",
      headers: {
        accept: "application/json",
        "heart-beat": "10000,10000",
        // Autenticação do handshake via ticket na URL (WsTicketAuthenticationFilter) —
        // não é necessário Authorization no frame CONNECT (RNF-003).
      },
      body: "",
    };

    this.ws.send(this._serializarFrame(connectFrame));
  }

  /** Subscrever ao tópico de board após CONNECTED */
  private _assinarBoard(): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;

    const subscribeFrame: StompFrame = {
      command: "SUBSCRIBE",
      headers: {
        id: `sub-board-${this.projetoId}`,
        destination: `/topic/board/${this.projetoId}`,
        ack: "auto",
      },
      body: "",
    };

    this.ws.send(this._serializarFrame(subscribeFrame));
  }

  private _processarMensagemStomp(frameStr: string): void {
    const frame = this._deserializarFrame(frameStr);

    if (frame.command === "CONNECTED") {
      // STOMP está pronto; assinar ao tópico
      this._assinarBoard();
      return;
    }

    if (frame.command === "MESSAGE") {
      try {
        const evento = JSON.parse(frame.body) as EventoBoardMessage;

        // Detectar gap de sequência
        if (evento.seq > this.ultimoSeq + 1 && this.ultimoSeq > 0) {
          console.warn(
            `[Board] Gap de sequência detectado: esperado ${this.ultimoSeq + 1}, recebido ${evento.seq}`
          );
          this.config.onRessinc?.("Gap de sequência");
          this._resincronizar();
          return;
        }

        this.ultimoSeq = evento.seq;
        this.config.onMensagem?.(evento);
      } catch (e) {
        console.error("[Board] Erro ao processar mensagem STOMP:", e);
      }
    }

    if (frame.command === "ERROR") {
      const erro = new Error(frame.body);
      this.config.onErro?.(erro);
    }
  }

  private _reconectar(): void {
    if (this.encerrado) return;
    if (!this.wsConnected) {
      this.tentativasReconexao++;
      // Backoff exponencial: 1s, 2s, 4s, 8s, 16s, 30s (teto)
      const delay = Math.min(1000 * Math.pow(2, this.tentativasReconexao - 1), 30000);
      console.warn(
        `[Board] Reconectando em ${delay}ms (tentativa ${this.tentativasReconexao})`
      );

      this.reconectTimer = setTimeout(() => {
        this.conectar().catch((e) => {
          console.error("[Board] Falha na reconexão:", e);
          // Continua tentando automaticamente
          this._reconectar();
        });
      }, delay);
    }
  }

  async _resincronizar(): Promise<void> {
    // C2 FIX: Recarrega estado do board — componente deve pausar eventos até terminar
    try {
      const response = await carregarBoard(this.projetoId);
      console.log("[Board] Resincronização completa", response);
    } catch (e) {
      // I4 FIX: Notifica usuário de erro de resincronização
      const erro = e instanceof Error ? e : new Error(String(e));
      console.error("[Board] Erro ao resincronizar:", erro);
      this.config.onErro?.(erro);
    }
  }

  desconectar(): void {
    this.encerrado = true;
    if (this.reconectTimer) {
      clearTimeout(this.reconectTimer);
    }
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
    if (this.ws) {
      try {
        if (this.ws.readyState === WebSocket.OPEN) {
          const disconnectFrame: StompFrame = {
            command: "DISCONNECT",
            headers: { receipt: "disconnect-0" },
            body: "",
          };
          this.ws.send(this._serializarFrame(disconnectFrame));
        }
        this.ws.close();
      } catch {
        /* noop */
      }
    }
    this.wsConnected = false;
  }

  /**
   * Serializar frame STOMP para string.
   * Formato: COMMAND\nheader1:value1\nheader2:value2\n\nBODY\0
   */
  private _serializarFrame(frame: StompFrame): string {
    let result = frame.command + "\n";
    for (const [key, value] of Object.entries(frame.headers)) {
      result += `${key}:${value}\n`;
    }
    result += "\n" + frame.body + "\0";
    return result;
  }

  /**
   * Deserializar frame STOMP de string.
   */
  private _deserializarFrame(frameStr: string): StompFrame {
    const nullIndex = frameStr.indexOf("\0");
    const frameData = nullIndex >= 0 ? frameStr.substring(0, nullIndex) : frameStr;
    const [headerStr, ...bodyParts] = frameData.split("\n\n");
    const [command, ...headerLines] = headerStr.split("\n");
    const headers: StompFrameHeader = {};

    for (const line of headerLines) {
      const [key, ...valueParts] = line.split(":");
      if (key && valueParts.length > 0) {
        headers[key] = valueParts.join(":");
      }
    }

    return {
      command: command.trim(),
      headers,
      body: bodyParts.join("\n\n"),
    };
  }
}
