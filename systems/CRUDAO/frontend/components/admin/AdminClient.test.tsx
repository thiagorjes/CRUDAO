import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { AdminClient } from "./AdminClient";

vi.mock("next/navigation", () => ({ useRouter: () => ({ refresh: vi.fn() }) }));

const projetoAtivo = { id: "p1", nome: "Projeto X", descricao: "desc", status: "ATIVO" as const };
const projetoFinalizado = { ...projetoAtivo, status: "FINALIZADO" as const };
const workflow = {
  id: "w1",
  nome: "Workflow padrão",
  etapas: [
    { id: "e1", nome: "A Fazer", ordem: 1, etapaFinal: false, transicoesSaida: ["e2"] },
    { id: "e2", nome: "Concluído", ordem: 2, etapaFinal: true, transicoesSaida: [] },
  ],
};

describe("AdminClient", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("sem workflow, mostra estado vazio com botão de criar", () => {
    render(<AdminClient projeto={projetoAtivo} workflow={null} raias={[]} />);

    expect(screen.getByText("Este projeto ainda não possui workflow configurado.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Criar workflow" })).toBeInTheDocument();
  });

  it("cria workflow via POST /api/projetos/{id}/workflows", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 201 }));
    render(<AdminClient projeto={projetoAtivo} workflow={null} raias={[]} />);

    fireEvent.click(screen.getByRole("button", { name: "Criar workflow" }));

    await vi.waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        "/api/projetos/p1/workflows",
        expect.objectContaining({ method: "POST" }),
      ),
    );
    expect(await screen.findByText("Workflow criado com sucesso.")).toBeInTheDocument();
  });

  it("com workflow, lista colunas na aba padrão e permite excluir workflow", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 200 }));
    render(<AdminClient projeto={projetoAtivo} workflow={workflow} raias={[]} />);

    expect(screen.getByText("A Fazer")).toBeInTheDocument();
    expect(screen.getByText("Concluído")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Excluir workflow" }));
    fireEvent.click(within(screen.getByRole("alertdialog")).getByRole("button", { name: "Excluir" }));

    await vi.waitFor(() =>
      expect(fetch).toHaveBeenCalledWith("/api/workflows/w1", expect.objectContaining({ method: "DELETE" })),
    );
  });

  it("botão 'Finalizar projeto' chama POST /api/projetos/{id}/finalizar", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 200 }));
    render(<AdminClient projeto={projetoAtivo} workflow={workflow} raias={[]} />);

    fireEvent.click(screen.getByRole("button", { name: "Finalizar projeto" }));

    await vi.waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        "/api/projetos/p1/finalizar",
        expect.objectContaining({ method: "POST" }),
      ),
    );
  });

  it("projeto finalizado mostra aviso de RN-015 e desabilita edição/'Reabrir projeto'", () => {
    render(<AdminClient projeto={projetoFinalizado} workflow={workflow} raias={[]} />);

    expect(
      screen.getByText(/reabra para editar workflow, etapas, transições ou raias/i),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Reabrir projeto" })).toBeInTheDocument();
    expect(screen.getByLabelText("Nome")).toBeDisabled();
  });
});
