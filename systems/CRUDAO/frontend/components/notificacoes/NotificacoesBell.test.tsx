import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { NotificacoesBell } from "./NotificacoesBell";

// useNotificacoesRealtime abre uma conexão STOMP/SockJS real (fora do escopo deste componente,
// já coberto pelo padrão equivalente de useBoardRealtime em TASK-07.2) — mockado para isolar o
// comportamento de fetch inicial + marcar como lida.
vi.mock("./useNotificacoesRealtime", () => ({
  useNotificacoesRealtime: vi.fn(),
}));

const notificacao = {
  id: "n1",
  tarefaId: "t1",
  tipo: "IMPEDIMENTO",
  mensagem: "Tarefa X foi impedida",
  lida: false,
  criadoEm: "2026-08-26T10:00:00Z",
};

describe("NotificacoesBell", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("busca notificações não lidas ao montar e mostra o badge de contagem", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify([notificacao]), { status: 200 }));

    render(<NotificacoesBell usuarioId="u1" backendPublicUrl="http://backend" />);

    expect(await screen.findByText("1")).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledWith("/api/notificacoes?apenasNaoLidas=true");
  });

  it("abre o painel e marca uma notificação como lida, removendo-a da lista", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify([notificacao]), { status: 200 }));
    render(<NotificacoesBell usuarioId="u1" backendPublicUrl="http://backend" />);
    await screen.findByText("1");

    fireEvent.click(screen.getByRole("button", { name: "Notificações" }));
    expect(screen.getByText("Tarefa X foi impedida")).toBeInTheDocument();

    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 200 }));
    fireEvent.click(screen.getByRole("button", { name: "Marcar como lida" }));

    await vi.waitFor(() =>
      expect(screen.queryByText("Tarefa X foi impedida")).not.toBeInTheDocument(),
    );
    expect(fetch).toHaveBeenCalledWith("/api/notificacoes/n1/lida", { method: "POST" });
  });

  it("reverte a remoção quando marcar como lida falha", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify([notificacao]), { status: 200 }));
    render(<NotificacoesBell usuarioId="u1" backendPublicUrl="http://backend" />);
    await screen.findByText("1");
    fireEvent.click(screen.getByRole("button", { name: "Notificações" }));

    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 500 }));
    fireEvent.click(screen.getByRole("button", { name: "Marcar como lida" }));

    await vi.waitFor(() =>
      expect(screen.getByText("Tarefa X foi impedida")).toBeInTheDocument(),
    );
  });
});
