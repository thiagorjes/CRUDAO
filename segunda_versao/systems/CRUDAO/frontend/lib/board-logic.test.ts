import { describe, expect, it } from "vitest";
import { EtapaResponse, TarefaBoardItemResponse } from "./board";
import {
  classeDestaque,
  etapaDeMenorOrdem,
  mensagemErro,
  tarefasDe,
  transicaoPermitida,
} from "./board-logic";

const aFazer: EtapaResponse = {
  id: "a-fazer",
  nome: "A Fazer",
  ordem: 1,
  etapaFinal: false,
  transicoesSaida: ["em-andamento"],
};
const emAndamento: EtapaResponse = {
  id: "em-andamento",
  nome: "Em Andamento",
  ordem: 2,
  etapaFinal: false,
  transicoesSaida: ["concluido"],
};
const concluido: EtapaResponse = {
  id: "concluido",
  nome: "Concluído",
  ordem: 3,
  etapaFinal: true,
  transicoesSaida: [],
};

const etapaPorId = new Map([aFazer, emAndamento, concluido].map((e) => [e.id, e]));

function tarefa(overrides: Partial<TarefaBoardItemResponse>): TarefaBoardItemResponse {
  return {
    id: "t1",
    titulo: "Tarefa",
    etapaAtualId: aFazer.id,
    raiaId: "raia-1",
    responsavelId: null,
    impedida: false,
    impedidaDesde: null,
    iniciada: false,
    ...overrides,
  };
}

describe("transicaoPermitida", () => {
  it("permite quando há transição configurada (RF-002)", () => {
    expect(transicaoPermitida(etapaPorId, aFazer.id, emAndamento.id)).toBe(true);
  });

  it("bloqueia quando não há transição configurada (pular etapa, RN-003)", () => {
    expect(transicaoPermitida(etapaPorId, aFazer.id, concluido.id)).toBe(false);
  });

  it("bloqueia mover para a própria etapa", () => {
    expect(transicaoPermitida(etapaPorId, aFazer.id, aFazer.id)).toBe(false);
  });

  it("bloqueia quando a etapa de origem é desconhecida", () => {
    expect(transicaoPermitida(etapaPorId, "inexistente", emAndamento.id)).toBe(false);
  });
});

describe("classeDestaque", () => {
  it("sem arrasto em andamento não destaca nenhuma coluna", () => {
    expect(classeDestaque(null, emAndamento.id, etapaPorId)).toBe("");
  });

  it("não destaca a própria coluna de origem", () => {
    expect(classeDestaque({ etapaOrigemId: aFazer.id }, aFazer.id, etapaPorId)).toBe("");
  });

  it("destaca como válida a coluna com transição configurada", () => {
    expect(classeDestaque({ etapaOrigemId: aFazer.id }, emAndamento.id, etapaPorId)).toBe(
      "column--drop-valid",
    );
  });

  it("destaca como inválida a coluna sem transição configurada", () => {
    expect(classeDestaque({ etapaOrigemId: aFazer.id }, concluido.id, etapaPorId)).toBe(
      "column--drop-invalid",
    );
  });
});

describe("etapaDeMenorOrdem", () => {
  it("retorna a etapa de menor ordem independente da ordem de entrada (RN-CB-004/005)", () => {
    expect(etapaDeMenorOrdem([concluido, aFazer, emAndamento])?.id).toBe(aFazer.id);
  });

  it("retorna undefined para lista vazia", () => {
    expect(etapaDeMenorOrdem([])).toBeUndefined();
  });
});

describe("tarefasDe", () => {
  const tarefas = [
    tarefa({ id: "t1", raiaId: "r1", etapaAtualId: aFazer.id }),
    tarefa({ id: "t2", raiaId: "r1", etapaAtualId: emAndamento.id }),
    tarefa({ id: "t3", raiaId: "r2", etapaAtualId: aFazer.id }),
  ];

  it("filtra por raia e etapa simultaneamente", () => {
    expect(tarefasDe(tarefas, "r1", aFazer.id).map((t) => t.id)).toEqual(["t1"]);
  });

  it("retorna vazio quando não há tarefas na combinação (RF-011)", () => {
    expect(tarefasDe(tarefas, "r2", emAndamento.id)).toEqual([]);
  });
});

describe("mensagemErro", () => {
  it("409 explica que a transição não está configurada (critério de aceite TASK-07.2)", () => {
    expect(mensagemErro(409, "Não foi possível mover o card.")).toContain(
      "transição não está configurada",
    );
  });

  it("403 explica falta de permissão", () => {
    expect(mensagemErro(403, "Não foi possível excluir o card.")).toContain("permissão");
  });

  it("outros status devolvem só a mensagem padrão", () => {
    expect(mensagemErro(500, "Não foi possível excluir o card.")).toBe(
      "Não foi possível excluir o card.",
    );
  });
});
