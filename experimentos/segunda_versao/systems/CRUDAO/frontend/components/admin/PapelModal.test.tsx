import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { PapelModal } from "./PapelModal";

function renderModal(overrides: Partial<Parameters<typeof PapelModal>[0]> = {}) {
  const props = {
    projetoId: "proj1",
    papel: null,
    onFechar: vi.fn(),
    onSalvo: vi.fn(),
    onErro: vi.fn(),
    ...overrides,
  };
  render(<PapelModal {...props} />);
  return props;
}

describe("PapelModal", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("bloqueia o envio quando chave/nome estão vazios na criação", async () => {
    const props = renderModal();

    fireEvent.submit(screen.getByRole("form", { name: "Formulário de papel" }));

    expect(await screen.findByText("Informe chave e nome do papel.")).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalled();
    expect(props.onSalvo).not.toHaveBeenCalled();
  });

  it("criação envia POST /api/projetos/{id}/papeis", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 201 }));
    const props = renderModal();

    fireEvent.change(screen.getByLabelText("Chave *"), { target: { value: "gestor" } });
    fireEvent.change(screen.getByLabelText("Nome *"), { target: { value: "Gestor" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await vi.waitFor(() => expect(props.onSalvo).toHaveBeenCalled());
    expect(fetch).toHaveBeenCalledWith("/api/projetos/proj1/papeis", expect.objectContaining({ method: "POST" }));
  });

  it("edição não mostra campo chave e envia PUT /api/papeis/{id}", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 200 }));
    const papel = { id: "p1", chave: "dev", nome: "Dev", protegido: false, permissoes: [] };
    const props = renderModal({ papel });

    expect(screen.queryByLabelText("Chave *")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await vi.waitFor(() => expect(props.onSalvo).toHaveBeenCalled());
    expect(fetch).toHaveBeenCalledWith("/api/papeis/p1", expect.objectContaining({ method: "PUT" }));
  });

  it("em 422 chama onErro com mensagem de chave reservada", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 422 }));
    const props = renderModal();

    fireEvent.change(screen.getByLabelText("Chave *"), { target: { value: "admin" } });
    fireEvent.change(screen.getByLabelText("Nome *"), { target: { value: "Admin" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await vi.waitFor(() => expect(props.onErro).toHaveBeenCalledWith("Chave 'admin' é reservada."));
  });
});
