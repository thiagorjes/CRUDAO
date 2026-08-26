import { NextRequest, NextResponse } from "next/server";
import { apiProxyFetch } from "./api";

/** Encaminha uma requisição do browser para o backend, sempre com Bearer server-side (TASK-07.2). */
export async function forwardToBackend(
  req: NextRequest,
  backendPath: string,
  method: string,
): Promise<NextResponse> {
  const temCorpo = method !== "GET" && method !== "DELETE";
  const corpo = temCorpo ? await req.text() : undefined;

  const res = await apiProxyFetch(backendPath, {
    method,
    headers: corpo ? { "Content-Type": "application/json" } : undefined,
    body: corpo || undefined,
  });

  const texto = await res.text();
  const contentType = res.headers.get("Content-Type");
  return new NextResponse(texto || null, {
    status: res.status,
    headers: contentType ? { "Content-Type": contentType } : undefined,
  });
}
