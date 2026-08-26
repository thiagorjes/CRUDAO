import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { env } from "./env";
import { SESSION_COOKIE, decifrarSessao } from "./session";

export async function obterSessao() {
  const store = await cookies();
  const bruto = store.get(SESSION_COOKIE)?.value;
  if (!bruto) return null;
  return decifrarSessao(bruto);
}

/**
 * Chama a API do backend a partir de Server Components/Route Handlers, sempre com Bearer — nunca
 * exposto ao JS do browser (decisão TASK-07.1). Renovação do access token perto de expirar (fora
 * da janela já tratada pelo middleware, ex.: chamadas subsequentes na mesma navegação) é só
 * best-effort aqui: Server Components não podem persistir cookies, então uma sessão que expire no
 * meio de uma renderização ainda cai em 401 — tratado abaixo redirecionando ao login.
 */
export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const sessao = await obterSessao();
  if (!sessao) {
    redirect("/login");
  }

  const res = await fetch(`${env.backendUrl()}${path}`, {
    ...init,
    headers: { ...(init.headers ?? {}), Authorization: `Bearer ${sessao.accessToken}` },
    cache: "no-store",
  });

  if (res.status === 401) {
    redirect("/login");
  }

  return res;
}

export async function apiFetchJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await apiFetch(path, init);
  if (!res.ok) {
    throw new Error(`Chamada a ${path} falhou com status ${res.status}`);
  }
  return res.json();
}

/**
 * Igual a `apiFetch`, mas para Route Handlers que fazem *proxy* de uma mutação disparada pelo
 * browser (criar/mover/excluir card etc. — TASK-07.2): nunca chama `redirect()`, porque quem
 * chamou é `fetch()` do lado do cliente (drag-and-drop, botão), não uma navegação — devolve a
 * resposta (incl. 401/403/409) para o Route Handler repassar como JSON ao browser.
 */
export async function apiProxyFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const sessao = await obterSessao();
  if (!sessao) {
    return new Response(JSON.stringify({ error: "unauthenticated" }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    });
  }

  return fetch(`${env.backendUrl()}${path}`, {
    ...init,
    headers: { ...(init.headers ?? {}), Authorization: `Bearer ${sessao.accessToken}` },
    cache: "no-store",
  });
}
