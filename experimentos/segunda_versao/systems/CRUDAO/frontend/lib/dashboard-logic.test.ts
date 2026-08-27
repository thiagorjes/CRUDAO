import { describe, expect, it } from "vitest";
import { formatarDuracao, ordenarPorLeadTimeDesc } from "./dashboard-logic";

describe("formatarDuracao", () => {
  it("retorna 0s para zero ou negativo", () => {
    expect(formatarDuracao(0)).toBe("0s");
    expect(formatarDuracao(-5)).toBe("0s");
  });

  it("formata segundos puros", () => {
    expect(formatarDuracao(45)).toBe("45s");
  });

  it("formata minutos", () => {
    expect(formatarDuracao(150)).toBe("2min");
  });

  it("formata horas com minutos", () => {
    expect(formatarDuracao(3600 + 5 * 60)).toBe("1h 5min");
  });

  it("formata horas exatas sem minutos", () => {
    expect(formatarDuracao(3600 * 2)).toBe("2h");
  });

  it("formata dias com horas", () => {
    expect(formatarDuracao(86400 + 3600 * 3)).toBe("1d 3h");
  });

  it("formata dias exatos sem horas", () => {
    expect(formatarDuracao(86400 * 2)).toBe("2d");
  });
});

describe("ordenarPorLeadTimeDesc", () => {
  it("ordena por leadTimeMedioSegundos decrescente sem mutar o array original", () => {
    const etapas = [
      { etapaId: "a", etapaNome: "A", leadTimeMedioSegundos: 100, tempoImpedimentoMedioSegundos: 0 },
      { etapaId: "b", etapaNome: "B", leadTimeMedioSegundos: 300, tempoImpedimentoMedioSegundos: 0 },
      { etapaId: "c", etapaNome: "C", leadTimeMedioSegundos: 200, tempoImpedimentoMedioSegundos: 0 },
    ];
    const original = [...etapas];

    const ordenado = ordenarPorLeadTimeDesc(etapas);

    expect(ordenado.map((e) => e.etapaId)).toEqual(["b", "c", "a"]);
    expect(etapas).toEqual(original);
  });
});
