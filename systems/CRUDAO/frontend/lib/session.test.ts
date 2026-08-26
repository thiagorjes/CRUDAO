import { beforeAll, describe, expect, it } from "vitest";

beforeAll(() => {
  process.env.SESSION_SECRET = "segredo-de-teste-nao-usar-em-producao";
  process.env.APP_URL = "http://localhost:3000";
});

describe("cifrarSessao / decifrarSessao", () => {
  it("round-trip preserva os dados da sessão", async () => {
    const { cifrarSessao, decifrarSessao } = await import("./session");
    const dados = {
      accessToken: "access-123",
      refreshToken: "refresh-456",
      idToken: "id-789",
      expiresAt: Date.now() + 60_000,
    };

    const cifrado = await cifrarSessao(dados);
    expect(cifrado).not.toContain("access-123"); // nunca em claro no cookie

    const decifrado = await decifrarSessao(cifrado);
    expect(decifrado).toMatchObject(dados);
  });

  it("retorna null para um token corrompido/adulterado", async () => {
    const { cifrarSessao, decifrarSessao } = await import("./session");
    const cifrado = await cifrarSessao({ accessToken: "x", expiresAt: Date.now() });
    const adulterado = cifrado.slice(0, -4) + "abcd";

    await expect(decifrarSessao(adulterado)).resolves.toBeNull();
  });

  it("retorna null para uma string que não é um JWE válido", async () => {
    const { decifrarSessao } = await import("./session");
    await expect(decifrarSessao("nao-e-um-token")).resolves.toBeNull();
  });

  it("não decifra com uma chave (SESSION_SECRET) diferente da usada para cifrar", async () => {
    const { cifrarSessao, decifrarSessao } = await import("./session");
    const cifrado = await cifrarSessao({ accessToken: "x", expiresAt: Date.now() });

    // chaveDerivada() lê env.sessionSecret() a cada chamada — não há cache de módulo a driblar.
    process.env.SESSION_SECRET = "outro-segredo-completamente-diferente";
    await expect(decifrarSessao(cifrado)).resolves.toBeNull();
  });
});
