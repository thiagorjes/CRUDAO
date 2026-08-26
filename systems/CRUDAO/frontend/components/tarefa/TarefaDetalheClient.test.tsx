import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { EtapaResponse, MembroProjeto } from "@/lib/board";
import { TarefaAuditoriaResponse, TarefaDetalheResponse } from "@/lib/tarefa";
import { TarefaDetalheClient } from "./TarefaDetalheClient";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ refresh: vi.fn() }),
}));

const etapas: EtapaResponse[] = [
  { id: "e1", nome: "A Fazer", ordem: 1, etapaFinal: false, transicoesSaida: ["e2"] },
  { id: "e2", nome: "Em Andamento", ordem: 2, etapaFinal: false, transicoesSaida: [] },
];
const membros: MembroProjeto[] = [
  { usuarioId: "u1", nome: "João Silva", email: "joao@ex.com", papeis: ["dev"] },
  { usuarioId: "u2", nome: "Maria Souza", email: "maria@ex.com", papeis: ["dev"] },
];

function tarefa(overrides: Partial<TarefaDetalheResponse> = {}): TarefaDetalheResponse {
  return {
    id: "t1",
    titulo: "Corrigir timeout",
    descricaoEscopo: "Timeout intermitente no gateway.",
    etapaAtualId: "e1",
    raiaId: "r1",
    responsavelId: "u1",
    iniciada: false,
    impedida: false,
    impedidaDesde: null,
    historicoEtapas: [
      { etapaId: "e1", entradaEm: "2026-08-24T09:00:00Z", saidaEm: null, leadTimeSegundos: 7200 },
    ],
    tempoImpedimentoTotalSegundos: 0,
    ...overrides,
  };
}

function renderComponente(overrides: {
  tarefa?: TarefaDetalheResponse;
  observadoresIniciais?: string[];
  auditoria: TarefaAuditoriaResponse[] | null;
}) {
  const props = {
    projetoId: "p1",
    tarefa: overrides.tarefa ?? tarefa(),
    etapas,
    membros,
    observadoresIniciais: overrides.observadoresIniciais ?? [],
    auditoria: overrides.auditoria,
  };
  return render(<TarefaDetalheClient {...props} />);
}

describe("TarefaDetalheClient", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("campos estruturais ficam bloqueados quando a tarefa já foi iniciada (RN-016)", () => {
    renderComponente({ tarefa: tarefa({ iniciada: true }), auditoria: [] });

    expect(screen.queryByLabelText("Título")).not.toBeInTheDocument();
    expect(screen.getByText("Corrigir timeout", { selector: ".field-locked" })).toBeInTheDocument();
    expect(
      screen.getByText("Timeout intermitente no gateway.", { selector: ".field-locked" }),
    ).toBeInTheDocument();
  });

  it("campos estruturais editáveis quando a tarefa não foi iniciada", () => {
    renderComponente({ tarefa: tarefa({ iniciada: false }), auditoria: [] });

    expect(screen.getByLabelText("Título")).toBeInTheDocument();
    expect(screen.getByLabelText(/^Descrição/)).toBeInTheDocument();
  });

  it("exibe lead-time por etapa, incluindo etapa em andamento (RF-006)", () => {
    renderComponente({ auditoria: [] });

    expect(screen.getByText(/A Fazer: 2h 0min \(em andamento\)/)).toBeInTheDocument();
  });

  it("oculta o histórico de auditoria quando o backend nega acesso (403 → null)", () => {
    renderComponente({ auditoria: null });

    expect(screen.queryByRole("region", { name: "Histórico de auditoria" })).not.toBeInTheDocument();
  });

  it("exibe o histórico de auditoria em ordem cronológica decrescente (RF-017)", () => {
    renderComponente({
      auditoria: [
        { autorId: "u1", campo: "etapa", valorAnterior: "Backlog", valorNovo: "A Fazer", dataHora: "2026-08-24T09:10:00Z" },
        { autorId: "u2", campo: "responsavel", valorAnterior: "—", valorNovo: "João Silva", dataHora: "2026-08-25T14:20:00Z" },
      ],
    });

    const itens = screen.getAllByText(/alterou/);
    expect(itens[0]).toHaveTextContent("responsavel");
    expect(itens[1]).toHaveTextContent("etapa");
  });

  it("envia PUT preservando descrição vazia ao limpar o campo (achado de code review)", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 200 }));
    renderComponente({ auditoria: [] });

    fireEvent.change(screen.getByLabelText(/^Descrição/), { target: { value: "" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await vi.waitFor(() => expect(fetch).toHaveBeenCalled());
    const corpo = JSON.parse(vi.mocked(fetch).mock.calls[0][1]!.body as string);
    expect(corpo.descricaoEscopo).toBe("");
    expect("descricaoEscopo" in corpo).toBe(true);
  });

  it("adiciona observador via POST /api/tarefas/{id}/observadores", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 201 }));
    renderComponente({ auditoria: [] });

    fireEvent.change(screen.getByLabelText("Adicionar observador"), { target: { value: "u2" } });
    fireEvent.click(screen.getByRole("button", { name: "Adicionar" }));

    await vi.waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        "/api/tarefas/t1/observadores",
        expect.objectContaining({ method: "POST" }),
      ),
    );
    expect(await screen.findByRole("button", { name: "Remover observador Maria Souza" })).toBeInTheDocument();
  });
});
