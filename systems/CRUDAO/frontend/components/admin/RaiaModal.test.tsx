import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { RaiaModal } from "./RaiaModal";

function renderModal(overrides: Partial<Parameters<typeof RaiaModal>[0]> = {}) {
  const props = {
    projetoId: "p1",
    raia: null,
    onFechar: vi.fn(),
    onSalvo: vi.fn(),
    onErro: vi.fn(),
    ...overrides,
  };
  render(<RaiaModal {...props} />);
  return props;
}

describe("RaiaModal", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("bloqueia o envio e mostra erro quando o nome está vazio", async () => {
    const props = renderModal();

    fireEvent.submit(screen.getByRole("form", { name: "Formulário de raia" }));

    expect(await screen.findByText("Informe o nome da raia.")).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalled();
    expect(props.onSalvo).not.toHaveBeenCalled();
  });

  it("criação envia POST /api/projetos/{id}/raias", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 201 }));
    const props = renderModal();

    fireEvent.change(screen.getByLabelText("Nome *"), { target: { value: "Backend" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await vi.waitFor(() => expect(props.onSalvo).toHaveBeenCalled());
    expect(fetch).toHaveBeenCalledWith(
      "/api/projetos/p1/raias",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("edição envia PUT /api/raias/{id}", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 200 }));
    const raia = { id: "r1", nome: "Backend", ordem: 1, global: false };
    const props = renderModal({ raia });

    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await vi.waitFor(() => expect(props.onSalvo).toHaveBeenCalled());
    expect(fetch).toHaveBeenCalledWith("/api/raias/r1", expect.objectContaining({ method: "PUT" }));
  });

  it("em erro chama onErro sem fechar o modal", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 409 }));
    const props = renderModal();

    fireEvent.change(screen.getByLabelText("Nome *"), { target: { value: "X" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await vi.waitFor(() => expect(props.onErro).toHaveBeenCalledWith("Não foi possível salvar a raia."));
  });
});
