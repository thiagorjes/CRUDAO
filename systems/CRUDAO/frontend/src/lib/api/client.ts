/**
 * Cliente HTTP para a API do backend, sempre via `/api/proxy` (RF-014, TASK-05.0) — nunca chama o
 * backend diretamente do browser, o proxy anexa o `Authorization: Bearer` a partir da sessão.
 */
export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
  }
}

async function requisicao<T>(path: string, init?: RequestInit): Promise<T> {
  const resposta = await fetch(`/api/proxy${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  });
  if (!resposta.ok) {
    const corpo = await resposta.text();
    throw new ApiError(corpo || `Erro HTTP ${resposta.status}`, resposta.status);
  }
  if (resposta.status === 204) {
    return undefined as T;
  }
  const texto = await resposta.text();
  return texto ? (JSON.parse(texto) as T) : (undefined as T);
}

export const api = {
  get: <T>(path: string) => requisicao<T>(path),
  post: <T>(path: string, body?: unknown) =>
    requisicao<T>(path, { method: 'POST', body: body !== undefined ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) =>
    requisicao<T>(path, { method: 'PUT', body: body !== undefined ? JSON.stringify(body) : undefined }),
  patch: <T>(path: string, body?: unknown) =>
    requisicao<T>(path, { method: 'PATCH', body: body !== undefined ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => requisicao<T>(path, { method: 'DELETE' }),
};
