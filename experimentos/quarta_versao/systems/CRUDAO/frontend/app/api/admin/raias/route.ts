import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "@/lib/api/proxy";

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const projetoId = searchParams.get("projetoId");

    if (!projetoId) {
      return NextResponse.json(
        { error: "projetoId é obrigatório" },
        { status: 400 }
      );
    }

    const response = await apiProxyFetch(`/api/projetos/${projetoId}/raias`, {
      method: "GET",
    });

    if (!response.ok) {
      return NextResponse.json(
        { error: "Erro ao obter raias" },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("[GET /api/admin/raias]", error);
    return NextResponse.json(
      { error: "Erro ao obter raias" },
      { status: 500 }
    );
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { projetoId, ...raiaData } = body;

    if (!projetoId) {
      return NextResponse.json(
        { error: "projetoId é obrigatório" },
        { status: 400 }
      );
    }

    const response = await apiProxyFetch(`/api/projetos/${projetoId}/raias`, {
      method: "POST",
      body: JSON.stringify(raiaData),
    });

    if (!response.ok) {
      const errorData = await response.json();
      return NextResponse.json(errorData, { status: response.status });
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("[POST /api/admin/raias]", error);
    return NextResponse.json(
      { error: "Erro ao criar raia" },
      { status: 500 }
    );
  }
}
