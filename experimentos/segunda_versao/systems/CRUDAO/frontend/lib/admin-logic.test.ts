import { describe, expect, it } from "vitest";
import { EtapaResponse } from "./board";
import {
  etapaSemTransicaoObrigatoria,
  mensagemErroAdmin,
  nomesTransicoesSaida,
  ordenarPorOrdem,
} from "./admin-logic";

function etapa(overrides: Partial<EtapaResponse> = {}): EtapaResponse {
  return {
    id: "e1",
    nome: "A Fazer",
    ordem: 1,
    etapaFinal: false,
    transicoesSaida: [],
    ...overrides,
  };
}

describe("etapaSemTransicaoObrigatoria", () => {
  it("etapa não-final sem transição de saída é inválida (RN-003)", () => {
    expect(etapaSemTransicaoObrigatoria(etapa())).toBe(true);
  });

  it("etapa não-final com transição de saída é válida", () => {
    expect(etapaSemTransicaoObrigatoria(etapa({ transicoesSaida: ["e2"] }))).toBe(false);
  });

  it("etapa final nunca precisa de transição de saída", () => {
    expect(etapaSemTransicaoObrigatoria(etapa({ etapaFinal: true }))).toBe(false);
  });
});

describe("nomesTransicoesSaida", () => {
  const etapaPorId = new Map<string, EtapaResponse>([
    ["e2", etapa({ id: "e2", nome: "Em Andamento" })],
    ["e3", etapa({ id: "e3", nome: "Concluído" })],
  ]);

  it("lista os nomes das etapas de destino", () => {
    expect(nomesTransicoesSaida(etapa({ transicoesSaida: ["e2", "e3"] }), etapaPorId)).toBe(
      "Em Andamento, Concluído",
    );
  });

  it("etapa não-final sem transição mostra travessão simples", () => {
    expect(nomesTransicoesSaida(etapa(), etapaPorId)).toBe("—");
  });

  it("etapa final sem transição indica que permite desfinalizar", () => {
    expect(nomesTransicoesSaida(etapa({ etapaFinal: true }), etapaPorId)).toBe(
      "— (permite desfinalizar)",
    );
  });
});

describe("ordenarPorOrdem", () => {
  it("ordena crescente por campo ordem sem mutar o array original", () => {
    const original = [{ ordem: 2 }, { ordem: 1 }];
    const ordenado = ordenarPorOrdem(original);
    expect(ordenado.map((i) => i.ordem)).toEqual([1, 2]);
    expect(original.map((i) => i.ordem)).toEqual([2, 1]);
  });
});

describe("mensagemErroAdmin", () => {
  it("409 menciona tarefas ativas vinculadas", () => {
    expect(mensagemErroAdmin(409, "Não foi possível excluir.")).toContain("tarefas ativas");
  });

  it("422 menciona validação de dados", () => {
    expect(mensagemErroAdmin(422, "Não foi possível salvar.")).toContain("Verifique os dados");
  });

  it("403 menciona permissão ou projeto finalizado", () => {
    expect(mensagemErroAdmin(403, "Não foi possível salvar.")).toContain("permissão");
  });

  it("outros status retornam a mensagem padrão", () => {
    expect(mensagemErroAdmin(500, "Não foi possível salvar.")).toBe("Não foi possível salvar.");
  });
});
