import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { EtapaModal } from "./EtapaModal";

function renderModal(overrides: Partial<Parameters<typeof EtapaModal>[0]> = {}) {
  const props = {
    workflowId: "w1",
    etapa: null,
    onFechar: vi.fn(),
    onSalvo: vi.fn(),
    onErro: vi.fn(),
    ...overrides,
  };
  render(<EtapaModal {...props} />);
  return props;
}

describe("EtapaModal", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("bloqueia o envio e mostra erro quando o nome está vazio", async () => {
    const props = renderModal();

    fireEvent.submit(screen.getByRole("form", { name: "Formulário de coluna" }));

    expect(await screen.findByText("Informe o nome da coluna.")).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalled();
    expect(props.onSalvo).not.toHaveBeenCalled();
  });

  it("criação envia POST /api/workflows/{id}/etapas", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 201 }));
    const props = renderModal();

    fireEvent.change(screen.getByLabelText("Nome *"), { target: { value: "Revisão" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await vi.waitFor(() => expect(props.onSalvo).toHaveBeenCalled());
    expect(fetch).toHaveBeenCalledWith(
      "/api/workflows/w1/etapas",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("edição envia PUT /api/etapas/{id} com os dados existentes pré-preenchidos", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 200 }));
    const etapa = { id: "e1", nome: "A Fazer", ordem: 1, etapaFinal: false, transicoesSaida: [] };
    const props = renderModal({ etapa });

    expect(screen.getByLabelText("Nome *")).toHaveValue("A Fazer");
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await vi.waitFor(() => expect(props.onSalvo).toHaveBeenCalled());
    expect(fetch).toHaveBeenCalledWith("/api/etapas/e1", expect.objectContaining({ method: "PUT" }));
  });

  it("em 422 chama onErro com mensagem de dados inválidos", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 422 }));
    const props = renderModal();

    fireEvent.change(screen.getByLabelText("Nome *"), { target: { value: "X" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await vi.waitFor(() => expect(props.onErro).toHaveBeenCalledWith("Dados inválidos para a coluna."));
  });
});
