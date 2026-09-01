import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

/**
 * BFF de projetos (nível raiz).
 * - GET  /api/projetos  → lista projetos (backend devolve todos para adminGlobal, vínculos para os demais).
 * - POST /api/projetos  → cria projeto (backend exige adminGlobal — RF-008 / RNF-003).
 */
export async function GET() {
  try {
    const response = await apiProxyFetch("/api/projetos", { method: "GET" });
    if (!response.ok) {
      return NextResponse.json(
        { error: "Erro ao listar projetos" },
        { status: response.status }
      );
    }
    return NextResponse.json(await response.json());
  } catch (error) {
    console.error("[GET /api/projetos]", error);
    return NextResponse.json({ error: "Erro ao listar projetos" }, { status: 500 });
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const response = await apiProxyFetch("/api/projetos", {
      method: "POST",
      body: JSON.stringify({
        nome: body?.nome,
        descricao: body?.descricao ?? null,
      }),
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      return NextResponse.json(data, { status: response.status });
    }
    return NextResponse.json(data, { status: 201 });
  } catch (error) {
    console.error("[POST /api/projetos]", error);
    return NextResponse.json({ error: "Erro ao criar projeto" }, { status: 500 });
  }
}
