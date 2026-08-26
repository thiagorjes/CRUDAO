import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { NovoCardModal } from "./NovoCardModal";

const raias = [{ id: "r1", nome: "Backend", ordem: 1, global: false }];
const membros = [{ usuarioId: "u1", nome: "João Silva", email: "joao@ex.com", papeis: ["dev"] }];

function renderModal(overrides: Partial<Parameters<typeof NovoCardModal>[0]> = {}) {
  const props = {
    projetoId: "p1",
    raias,
    membros,
    onFechar: vi.fn(),
    onCriado: vi.fn(),
    onErro: vi.fn(),
    ...overrides,
  };
  render(<NovoCardModal {...props} />);
  return props;
}

describe("NovoCardModal", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("bloqueia o envio e mostra erro de validação quando o título está vazio (TL-05)", async () => {
    const props = renderModal();

    // fireEvent.submit no <form>, não click no botão — o atributo `required` do input faz o jsdom
    // (como um browser real) bloquear o evento de submit antes de chegar no nosso onSubmit, então
    // clicar no botão nunca exercitaria a validação própria do componente.
    fireEvent.submit(screen.getByRole("form", { name: "Formulário de nova tarefa" }));

    expect(await screen.findByText("Informe o título da tarefa.")).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalled();
    expect(props.onCriado).not.toHaveBeenCalled();
  });

  it("envia POST /api/projetos/{id}/tarefas com o título preenchido e chama onCriado", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 201 }));
    const props = renderModal();

    fireEvent.change(screen.getByLabelText("Título *"), {
      target: { value: "Corrigir timeout" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Criar card" }));

    await vi.waitFor(() => expect(props.onCriado).toHaveBeenCalled());

    expect(fetch).toHaveBeenCalledWith(
      "/api/projetos/p1/tarefas",
      expect.objectContaining({ method: "POST" }),
    );
    const corpo = JSON.parse(vi.mocked(fetch).mock.calls[0][1]!.body as string);
    expect(corpo.titulo).toBe("Corrigir timeout");
  });

  it("em 403 chama onErro com mensagem de permissão, sem fechar o modal", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 403 }));
    const props = renderModal();

    fireEvent.change(screen.getByLabelText("Título *"), { target: { value: "X" } });
    fireEvent.click(screen.getByRole("button", { name: "Criar card" }));

    await vi.waitFor(() => expect(props.onErro).toHaveBeenCalled());
    expect(props.onErro).toHaveBeenCalledWith(expect.stringContaining("permissão"));
    expect(props.onCriado).not.toHaveBeenCalled();
  });
});
