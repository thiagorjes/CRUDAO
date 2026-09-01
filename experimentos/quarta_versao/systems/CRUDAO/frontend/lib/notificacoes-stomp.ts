/**
 * Cliente STOMP/WebSocket para push de notificações (RF-005).
 * Subscreve `/topic/notificacoes/{usuarioId}`; a autorização da subscrição é validada no backend
 * (BoardChannelInterceptor) — o cliente nunca é a barreira.
 *
 * Autenticação do handshake: ticket de curta duração obtido via `getTicket()` e passado como
 * `?ticket=` na URL (o browser não pode setar Authorization no upgrade nativo). Um ticket novo é
 * buscado a cada tentativa de conexão/reconexão.
 *
 * O payload do evento é apenas um gatilho (envelope enxuto): ao receber qualquer MESSAGE, o
 * consumidor recarrega a lista via GET /api/notificacoes.
 */

type FrameHeader = Record<string, string>;

interface Frame {
  command: string;
  headers: FrameHeader;
  body: string;
}

interface NotificacoesStompConfig {
  onEvento?: () => void;
  onErro?: (erro: Error) => void;
}

export class NotificacoesStomp {
  private readonly wsBaseUrl: string;
  private readonly usuarioId: string;
  private readonly getTicket: () => Promise<string>;
  private readonly config: NotificacoesStompConfig;

  private ws?: WebSocket;
  private conectado = false;
  private encerrado = false;
  private tentativas = 0;
  private reconectTimer?: ReturnType<typeof setTimeout>;

  constructor(
    wsBaseUrl: string,
    usuarioId: string,
    getTicket: () => Promise<string>,
    config?: NotificacoesStompConfig
  ) {
    this.wsBaseUrl = wsBaseUrl.replace(/\/$/, "");
    this.usuarioId = usuarioId;
    this.getTicket = getTicket;
    this.config = config ?? {};
  }

  async conectar(): Promise<void> {
    if (this.encerrado) return;
    try {
      const ticket = await this.getTicket();
      if (this.encerrado) return;
      this.ws = new WebSocket(`${this.wsBaseUrl}/ws?ticket=${encodeURIComponent(ticket)}`);

      this.ws.onopen = () => {
        this.tentativas = 0;
        this.enviarConnect();
      };
      this.ws.onclose = () => {
        this.conectado = false;
        this.agendarReconexao();
      };
      this.ws.onerror = () => {
        this.config.onErro?.(new Error("Erro na conexão WebSocket de notificações"));
      };
      this.ws.onmessage = (evt) => this.processar(String(evt.data));
    } catch (e) {
      this.config.onErro?.(e instanceof Error ? e : new Error(String(e)));
      this.agendarReconexao();
    }
  }

  desconectar(): void {
    this.encerrado = true;
    if (this.reconectTimer) clearTimeout(this.reconectTimer);
    if (this.ws) {
      try {
        if (this.ws.readyState === WebSocket.OPEN) {
          this.ws.send(this.serializar({ command: "DISCONNECT", headers: {}, body: "" }));
        }
        this.ws.close();
      } catch {
        /* noop */
      }
    }
    this.conectado = false;
  }

  private enviarConnect(): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    this.ws.send(
      this.serializar({
        command: "CONNECT",
        headers: { accept: "application/json", "heart-beat": "10000,10000" },
        body: "",
      })
    );
  }

  private assinar(): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    this.ws.send(
      this.serializar({
        command: "SUBSCRIBE",
        headers: {
          id: `sub-notif-${this.usuarioId}`,
          destination: `/topic/notificacoes/${this.usuarioId}`,
          ack: "auto",
        },
        body: "",
      })
    );
  }

  private processar(frameStr: string): void {
    const frame = this.deserializar(frameStr);
    if (frame.command === "CONNECTED") {
      this.conectado = true;
      this.assinar();
      return;
    }
    if (frame.command === "MESSAGE") {
      this.config.onEvento?.();
      return;
    }
    if (frame.command === "ERROR") {
      this.config.onErro?.(new Error(frame.body || "STOMP ERROR"));
    }
  }

  private agendarReconexao(): void {
    if (this.encerrado || this.conectado) return;
    this.tentativas++;
    const delay = Math.min(1000 * 2 ** (this.tentativas - 1), 30000);
    this.reconectTimer = setTimeout(() => {
      this.conectar().catch(() => this.agendarReconexao());
    }, delay);
  }

  private serializar(frame: Frame): string {
    let out = frame.command + "\n";
    for (const [k, v] of Object.entries(frame.headers)) out += `${k}:${v}\n`;
    out += "\n" + frame.body + "\0";
    return out;
  }

  private deserializar(frameStr: string): Frame {
    const nullIndex = frameStr.indexOf("\0");
    const data = nullIndex >= 0 ? frameStr.substring(0, nullIndex) : frameStr;
    const [headerStr, ...bodyParts] = data.split("\n\n");
    const [command, ...headerLines] = headerStr.split("\n");
    const headers: FrameHeader = {};
    for (const line of headerLines) {
      const [key, ...rest] = line.split(":");
      if (key && rest.length > 0) headers[key] = rest.join(":");
    }
    return { command: command.trim(), headers, body: bodyParts.join("\n\n") };
  }
}
