import { describe, expect, it } from "vitest";
import {
  autorPossuiPapel,
  mensagemErroPapeis,
  ordenarPapeisPorNome,
  ordenarUsuariosPorNome,
  papeisAssociaveis,
} from "./papeis-logic";
import { PapelResponse } from "./papeis";

const papel = (over: Partial<PapelResponse> = {}): PapelResponse => ({
  id: "p1",
  chave: "dev",
  nome: "Dev",
  protegido: false,
  permissoes: [],
  ...over,
});

describe("mensagemErroPapeis", () => {
  it("403 menciona RN-017", () => {
    expect(mensagemErroPapeis(403, "Não foi possível.")).toContain("RN-017");
  });

  it("409 menciona vínculo/chave duplicada", () => {
    expect(mensagemErroPapeis(409, "Não foi possível.")).toContain("vinculados");
  });

  it("422 menciona dados inválidos", () => {
    expect(mensagemErroPapeis(422, "Não foi possível.")).toContain("protegido");
  });

  it("status desconhecido retorna a mensagem padrão", () => {
    expect(mensagemErroPapeis(500, "Falhou.")).toBe("Falhou.");
  });
});

describe("papeisAssociaveis", () => {
  it("exclui papéis protegidos (RN-006)", () => {
    const papeis = [papel({ id: "1" }), papel({ id: "2", protegido: true, chave: "admin" })];
    expect(papeisAssociaveis(papeis)).toEqual([papeis[0]]);
  });
});

describe("autorPossuiPapel", () => {
  const usuarios = [{ usuarioId: "u1", papeis: ["dev", "gestor"] }];

  it("true quando o autor possui o papel", () => {
    expect(autorPossuiPapel(usuarios, "u1", "dev")).toBe(true);
  });

  it("false quando o autor não possui o papel", () => {
    expect(autorPossuiPapel(usuarios, "u1", "admin")).toBe(false);
  });

  it("false quando não há autor ou autor não está na lista", () => {
    expect(autorPossuiPapel(usuarios, null, "dev")).toBe(false);
    expect(autorPossuiPapel(usuarios, "u2", "dev")).toBe(false);
  });
});

describe("ordenarPapeisPorNome / ordenarUsuariosPorNome", () => {
  it("ordena alfabeticamente por nome", () => {
    const papeis = [papel({ nome: "Zeta" }), papel({ nome: "Alfa" })];
    expect(ordenarPapeisPorNome(papeis).map((p) => p.nome)).toEqual(["Alfa", "Zeta"]);

    const usuarios = [{ nome: "Zeta" }, { nome: "Alfa" }];
    expect(ordenarUsuariosPorNome(usuarios).map((u) => u.nome)).toEqual(["Alfa", "Zeta"]);
  });
});
