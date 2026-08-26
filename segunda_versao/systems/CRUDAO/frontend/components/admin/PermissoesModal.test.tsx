import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { PermissoesModal } from "./PermissoesModal";

const papel = {
  id: "p1",
  chave: "dev",
  nome: "Dev",
  protegido: false,
  permissoes: [
    { chave: "tarefa:gerenciar", habilitada: false },
    { chave: "tarefa:excluir", habilitada: true },
  ],
};

function renderModal(overrides: Partial<Parameters<typeof PermissoesModal>[0]> = {}) {
  const props = {
    papel,
    bloqueadoPorAutoconcessao: false,
    onFechar: vi.fn(),
    onErro: vi.fn(),
    ...overrides,
  };
  render(<PermissoesModal {...props} />);
  return props;
}

describe("PermissoesModal", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("alterna um toggle com PUT imediato", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 200 }));
    renderModal();

    const checkbox = screen.getByLabelText("tarefa:gerenciar") as HTMLInputElement;
    fireEvent.click(checkbox);

    await vi.waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        "/api/papeis/p1/permissoes/tarefa:gerenciar",
        expect.objectContaining({ method: "PUT" }),
      ),
    );
  });

  it("em erro, reverte o toggle e chama onErro (RN-017)", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 403 }));
    const props = renderModal();

    const checkbox = screen.getByLabelText("tarefa:gerenciar") as HTMLInputElement;
    fireEvent.click(checkbox);

    await vi.waitFor(() => expect(props.onErro).toHaveBeenCalled());
    expect(checkbox.checked).toBe(false);
  });

  it("desabilita todos os toggles quando bloqueado por autoconcessão", () => {
    renderModal({ bloqueadoPorAutoconcessao: true });

    expect(screen.getByLabelText("tarefa:gerenciar")).toBeDisabled();
    expect(screen.getByLabelText("tarefa:excluir")).toBeDisabled();
    expect(screen.getByText(/RN-017/)).toBeInTheDocument();
  });

  it("desabilita todos os toggles quando o papel é protegido", () => {
    renderModal({ papel: { ...papel, protegido: true } });

    expect(screen.getByLabelText("tarefa:gerenciar")).toBeDisabled();
  });
});
