import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { ConfirmExcluirModal } from "./ConfirmExcluirModal";

describe("ConfirmExcluirModal", () => {
  it("mostra o título do card na pergunta de confirmação (TL-06)", () => {
    render(
      <ConfirmExcluirModal
        titulo="Corrigir timeout no gateway"
        onCancelar={vi.fn()}
        onConfirmar={vi.fn().mockResolvedValue(undefined)}
      />,
    );

    expect(screen.getByText(/Corrigir timeout no gateway/)).toBeInTheDocument();
  });

  it("cancelar chama onCancelar sem chamar onConfirmar", () => {
    const onCancelar = vi.fn();
    const onConfirmar = vi.fn().mockResolvedValue(undefined);
    render(<ConfirmExcluirModal titulo="X" onCancelar={onCancelar} onConfirmar={onConfirmar} />);

    fireEvent.click(screen.getByRole("button", { name: "Cancelar" }));

    expect(onCancelar).toHaveBeenCalled();
    expect(onConfirmar).not.toHaveBeenCalled();
  });

  it("excluir chama onConfirmar (RF-019)", async () => {
    const onConfirmar = vi.fn().mockResolvedValue(undefined);
    render(<ConfirmExcluirModal titulo="X" onCancelar={vi.fn()} onConfirmar={onConfirmar} />);

    fireEvent.click(screen.getByRole("button", { name: "Excluir" }));

    await vi.waitFor(() => expect(onConfirmar).toHaveBeenCalled());
  });
});
