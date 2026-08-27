import { describe, expect, it } from "vitest";
import { EtapaResponse } from "./board";
import { HistoricoEtapaResponse } from "./tarefa";
import {
  camposEstruturaisBloqueados,
  formatarDuracao,
  nomeEtapa,
  ordenarAuditoriaDesc,
  ordenarHistoricoEtapas,
} from "./tarefa-logic";

describe("formatarDuracao", () => {
  it("segundos zero ou negativos viram travessão", () => {
    expect(formatarDuracao(0)).toBe("—");
    expect(formatarDuracao(-5)).toBe("—");
  });

  it("formata em minutos quando menor que uma hora", () => {
    expect(formatarDuracao(125)).toBe("2min");
  });

  it("formata em horas e minutos quando menor que um dia", () => {
    expect(formatarDuracao(3 * 3600 + 45 * 60)).toBe("3h 45min");
  });

  it("formata em dias e horas quando maior que um dia (RF-006)", () => {
    expect(formatarDuracao(2 * 86400 + 4 * 3600)).toBe("2d 4h");
  });
});

describe("nomeEtapa", () => {
  const etapaPorId = new Map<string, EtapaResponse>([
    ["e1", { id: "e1", nome: "A Fazer", ordem: 1, etapaFinal: false, transicoesSaida: [] }],
  ]);

  it("retorna o nome da etapa conhecida", () => {
    expect(nomeEtapa(etapaPorId, "e1")).toBe("A Fazer");
  });

  it("retorna fallback para etapa desconhecida", () => {
    expect(nomeEtapa(etapaPorId, "inexistente")).toBe("Etapa desconhecida");
  });
});

describe("ordenarAuditoriaDesc", () => {
  it("ordena do mais recente para o mais antigo (RF-017)", () => {
    const itens = [
      { dataHora: "2026-08-24T09:10:00Z" },
      { dataHora: "2026-08-25T14:20:00Z" },
    ];
    expect(ordenarAuditoriaDesc(itens).map((i) => i.dataHora)).toEqual([
      "2026-08-25T14:20:00Z",
      "2026-08-24T09:10:00Z",
    ]);
  });
});

describe("ordenarHistoricoEtapas", () => {
  it("ordena por entrada, mais antiga primeiro (RF-006)", () => {
    const itens: HistoricoEtapaResponse[] = [
      { etapaId: "e2", entradaEm: "2026-08-25T00:00:00Z", saidaEm: null, leadTimeSegundos: 0 },
      { etapaId: "e1", entradaEm: "2026-08-24T00:00:00Z", saidaEm: "2026-08-25T00:00:00Z", leadTimeSegundos: 86400 },
    ];
    expect(ordenarHistoricoEtapas(itens).map((i) => i.etapaId)).toEqual(["e1", "e2"]);
  });
});

describe("camposEstruturaisBloqueados", () => {
  it("bloqueado quando tarefa iniciada (RN-016)", () => {
    expect(camposEstruturaisBloqueados(true)).toBe(true);
    expect(camposEstruturaisBloqueados(false)).toBe(false);
  });
});
