import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { TransicoesModal } from "./TransicoesModal";

const etapaOrigem = { id: "e1", nome: "A Fazer", ordem: 1, etapaFinal: false, transicoesSaida: ["e2"] };
const etapas = [
  etapaOrigem,
  { id: "e2", nome: "Em Andamento", ordem: 2, etapaFinal: false, transicoesSaida: [] },
  { id: "e3", nome: "Concluído", ordem: 3, etapaFinal: true, transicoesSaida: [] },
];

function renderModal(overrides: Partial<Parameters<typeof TransicoesModal>[0]> = {}) {
  const props = {
    etapa: etapaOrigem,
    etapas,
    onFechar: vi.fn(),
    onSalvo: vi.fn(),
    onErro: vi.fn(),
    ...overrides,
  };
  render(<TransicoesModal {...props} />);
  return props;
}

describe("TransicoesModal", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("pré-marca as transições já configuradas e não lista a própria etapa", () => {
    renderModal();

    expect(screen.getByLabelText("Em Andamento")).toBeChecked();
    expect(screen.getByLabelText("Concluído")).not.toBeChecked();
    expect(screen.queryByLabelText("A Fazer")).not.toBeInTheDocument();
  });

  it("salva o conjunto atualizado de transições via PUT /api/etapas/{id}/transicoes", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 200 }));
    const props = renderModal();

    fireEvent.click(screen.getByLabelText("Concluído"));
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await vi.waitFor(() => expect(props.onSalvo).toHaveBeenCalled());
    expect(fetch).toHaveBeenCalledWith(
      "/api/etapas/e1/transicoes",
      expect.objectContaining({ method: "PUT" }),
    );
    const corpo = JSON.parse(vi.mocked(fetch).mock.calls[0][1]!.body as string);
    expect(corpo.etapasDestinoIds.sort()).toEqual(["e2", "e3"]);
  });

  it("em 422 chama onErro com mensagem sobre RN-003", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 422 }));
    const props = renderModal();

    fireEvent.click(screen.getByLabelText("Em Andamento"));
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await vi.waitFor(() =>
      expect(props.onErro).toHaveBeenCalledWith(
        "Etapa não-final precisa de ao menos uma transição de saída.",
      ),
    );
  });
});
