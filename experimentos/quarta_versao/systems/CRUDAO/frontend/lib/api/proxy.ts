import { cookies } from "next/headers";

/**
 * Faz proxy de requisições ao backend, adicionando autenticação
 * Usado em route handlers de API
 */
export async function apiProxyFetch(
  path: string,
  options?: RequestInit
): Promise<Response> {
  const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL;

  if (!backendUrl) {
    throw new Error(
      "NEXT_PUBLIC_BACKEND_URL não configurada. Configure em .env.local ou variáveis de ambiente."
    );
  }

  const url = `${backendUrl}${path}`;

  // Obter token da sessão
  const cookieStore = await cookies();
  const session = cookieStore.get("session");
  const token = session?.value;

  // Montar headers com autenticação
  const headers = new Headers(options?.headers || {});
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  headers.set("Content-Type", "application/json");

  // Fazer requisição ao backend
  return fetch(url, {
    ...options,
    headers,
  });
}
