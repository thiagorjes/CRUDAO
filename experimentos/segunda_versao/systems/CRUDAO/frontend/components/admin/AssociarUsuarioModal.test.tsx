import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { AssociarUsuarioModal } from "./AssociarUsuarioModal";

const papeis = [{ id: "papel1", chave: "dev", nome: "Dev", protegido: false, permissoes: [] }];

function renderModal(overrides: Partial<Parameters<typeof AssociarUsuarioModal>[0]> = {}) {
  const props = {
    projetoId: "proj1",
    papeisAssociaveis: papeis,
    onFechar: vi.fn(),
    onSalvo: vi.fn(),
    onErro: vi.fn(),
    ...overrides,
  };
  render(<AssociarUsuarioModal {...props} />);
  return props;
}

describe("AssociarUsuarioModal", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
    vi.useFakeTimers({ shouldAdvanceTime: true });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("não busca com menos de 3 caracteres", async () => {
    renderModal();
    fireEvent.change(screen.getByLabelText("Buscar usuário (nome ou e-mail) *"), { target: { value: "an" } });
    await vi.advanceTimersByTimeAsync(400);
    expect(fetch).not.toHaveBeenCalled();
  });

  it("busca com debounce a partir de 3 caracteres e lista resultados", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(JSON.stringify([{ id: "u1", nome: "Ana Silva", email: "ana@ex.com" }]), { status: 200 }),
    );
    renderModal();

    fireEvent.change(screen.getByLabelText("Buscar usuário (nome ou e-mail) *"), { target: { value: "ana" } });
    await vi.advanceTimersByTimeAsync(400);

    expect(fetch).toHaveBeenCalledWith("/api/projetos/proj1/usuarios/buscar?q=ana");
    expect(await screen.findByText("Ana Silva <ana@ex.com>")).toBeInTheDocument();
  });

  it("bloqueia associar sem usuário selecionado", async () => {
    const props = renderModal();
    fireEvent.click(screen.getByRole("button", { name: "Associar" }));
    expect(await screen.findByText("Busque e selecione um usuário e um papel.")).toBeInTheDocument();
    expect(props.onSalvo).not.toHaveBeenCalled();
  });

  it("seleciona um resultado e associa via POST", async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(
        new Response(JSON.stringify([{ id: "u1", nome: "Ana Silva", email: "ana@ex.com" }]), { status: 200 }),
      )
      .mockResolvedValueOnce(new Response(null, { status: 201 }));
    const props = renderModal();

    fireEvent.change(screen.getByLabelText("Buscar usuário (nome ou e-mail) *"), { target: { value: "ana" } });
    await vi.advanceTimersByTimeAsync(400);
    fireEvent.click(await screen.findByText("Ana Silva <ana@ex.com>"));
    fireEvent.click(screen.getByRole("button", { name: "Associar" }));

    await vi.waitFor(() => expect(props.onSalvo).toHaveBeenCalled());
    expect(fetch).toHaveBeenLastCalledWith(
      "/api/projetos/proj1/usuarios",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ usuarioId: "u1", papelId: "papel1" }),
      }),
    );
  });
});
