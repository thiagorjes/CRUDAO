import { beforeEach, describe, expect, it, vi } from "vitest";

// carregarBoard é chamada pela resincronização; isolar do fetch real.
const carregarBoardMock = vi.fn().mockResolvedValue({ etapas: [], raias: [], tarefas: [] });
vi.mock("./lib/api/board", () => ({
  carregarBoard: (...args: unknown[]) => carregarBoardMock(...args),
}));

import { StompManager } from "./lib/stomp";

/** Monta um frame STOMP MESSAGE cru como o servidor enviaria (COMMAND\n\nBODY\0). */
function mensagemStomp(body: unknown): string {
  return `MESSAGE\n\n${JSON.stringify(body)}\0`;
}

function novoManager(config?: {
  onMensagem?: (e: unknown) => void;
  onRessinc?: (motivo: string) => void;
}) {
  return new StompManager(
    "ws://localhost:8081",
    "proj-1",
    async () => "ticket-fake",
    config as never
  );
}

describe("TASK-08.1 — StompManager: resincronização client-side por gap de seq (ADR-004)", () => {
  beforeEach(() => {
    carregarBoardMock.mockClear();
  });

  it("entrega eventos em sequência sem disparar resync", () => {
    const recebidos: number[] = [];
    const mgr = novoManager({ onMensagem: (e: any) => recebidos.push(e.seq) });
    const processar = (mgr as any)._processarMensagemStomp.bind(mgr);

    processar(mensagemStomp({ seq: 1, tipo: "TAREFA_CRIADA", projetoId: "proj-1" }));
    processar(mensagemStomp({ seq: 2, tipo: "TAREFA_MOVIDA", projetoId: "proj-1" }));

    expect(recebidos).toEqual([1, 2]);
    expect(carregarBoardMock).not.toHaveBeenCalled();
  });

  it("dispara onRessinc + GET /board quando há gap de sequência", async () => {
    const motivos: string[] = [];
    const mgr = novoManager({ onRessinc: (m) => motivos.push(m) });
    const processar = (mgr as any)._processarMensagemStomp.bind(mgr);

    processar(mensagemStomp({ seq: 1, tipo: "TAREFA_CRIADA", projetoId: "proj-1" }));
    // pula seq 2 e 3
    processar(mensagemStomp({ seq: 4, tipo: "TAREFA_MOVIDA", projetoId: "proj-1" }));

    expect(motivos).toEqual(["Gap de sequência"]);
    await vi.waitFor(() => expect(carregarBoardMock).toHaveBeenCalledWith("proj-1"));
  });

  it("não trata o primeiro evento (ultimoSeq=0) como gap", () => {
    const mgr = novoManager({});
    const processar = (mgr as any)._processarMensagemStomp.bind(mgr);

    processar(mensagemStomp({ seq: 7, tipo: "TAREFA_CRIADA", projetoId: "proj-1" }));

    expect(carregarBoardMock).not.toHaveBeenCalled();
    expect((mgr as any).ultimoSeq).toBe(7);
  });
});
