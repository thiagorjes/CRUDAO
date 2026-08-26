import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { ConfirmModal } from "./ConfirmModal";

describe("ConfirmModal", () => {
  it("chama onCancelar sem executar onConfirmar", () => {
    const onCancelar = vi.fn();
    const onConfirmar = vi.fn();
    render(
      <ConfirmModal titulo="Excluir raia" mensagem="Tem certeza?" onCancelar={onCancelar} onConfirmar={onConfirmar} />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Cancelar" }));

    expect(onCancelar).toHaveBeenCalled();
    expect(onConfirmar).not.toHaveBeenCalled();
  });

  it("chama onConfirmar ao clicar em Excluir", async () => {
    const onConfirmar = vi.fn().mockResolvedValue(undefined);
    render(<ConfirmModal titulo="Excluir raia" mensagem="Tem certeza?" onCancelar={vi.fn()} onConfirmar={onConfirmar} />);

    fireEvent.click(screen.getByRole("button", { name: "Excluir" }));

    await vi.waitFor(() => expect(onConfirmar).toHaveBeenCalled());
  });
});
