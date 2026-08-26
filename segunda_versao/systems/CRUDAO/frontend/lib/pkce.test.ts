import { describe, expect, it } from "vitest";
import { desafioPkce, gerarValorAleatorio } from "./pkce";

describe("gerarValorAleatorio", () => {
  it("gera valores distintos a cada chamada", () => {
    const a = gerarValorAleatorio(32);
    const b = gerarValorAleatorio(32);
    expect(a).not.toEqual(b);
  });

  it("gera string base64url (sem +, / ou = de padding)", () => {
    const valor = gerarValorAleatorio(32);
    expect(valor).toMatch(/^[A-Za-z0-9_-]+$/);
  });
});

describe("desafioPkce", () => {
  it("é determinístico para o mesmo code_verifier", async () => {
    const verifier = gerarValorAleatorio(32);
    const desafio1 = await desafioPkce(verifier);
    const desafio2 = await desafioPkce(verifier);
    expect(desafio1).toEqual(desafio2);
  });

  it("produz desafios diferentes para verifiers diferentes", async () => {
    const desafioA = await desafioPkce(gerarValorAleatorio(32));
    const desafioB = await desafioPkce(gerarValorAleatorio(32));
    expect(desafioA).not.toEqual(desafioB);
  });

  it("bate com o vetor de teste RFC 7636 (Apêndice B)", async () => {
    // https://datatracker.ietf.org/doc/html/rfc7636#appendix-B
    const verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    const desafioEsperado = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    await expect(desafioPkce(verifier)).resolves.toEqual(desafioEsperado);
  });
});
