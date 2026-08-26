import { describe, expect, it } from "vitest";
import { mesclarNotificacao, ordenarPorDataDesc } from "./notificacoes-logic";
import { NotificacaoResponse } from "./notificacoes";

function notif(overrides: Partial<NotificacaoResponse> = {}): NotificacaoResponse {
  return {
    id: "n1",
    tarefaId: "t1",
    tipo: "IMPEDIMENTO",
    mensagem: "Tarefa impedida",
    lida: false,
    criadoEm: "2026-08-26T10:00:00Z",
    ...overrides,
  };
}

describe("ordenarPorDataDesc", () => {
  it("ordena da mais recente para a mais antiga", () => {
    const antiga = notif({ id: "n1", criadoEm: "2026-08-25T10:00:00Z" });
    const recente = notif({ id: "n2", criadoEm: "2026-08-26T10:00:00Z" });

    expect(ordenarPorDataDesc([antiga, recente])).toEqual([recente, antiga]);
  });
});

describe("mesclarNotificacao", () => {
  it("insere uma notificação nova no topo", () => {
    const existente = notif({ id: "n1", criadoEm: "2026-08-25T10:00:00Z" });
    const nova = notif({ id: "n2", criadoEm: "2026-08-26T10:00:00Z" });

    expect(mesclarNotificacao([existente], nova)).toEqual([nova, existente]);
  });

  it("substitui (sem duplicar) quando o id já existe na lista", () => {
    const original = notif({ id: "n1", lida: false });
    const atualizada = notif({ id: "n1", lida: true });

    const resultado = mesclarNotificacao([original], atualizada);

    expect(resultado).toHaveLength(1);
    expect(resultado[0].lida).toBe(true);
  });
});
