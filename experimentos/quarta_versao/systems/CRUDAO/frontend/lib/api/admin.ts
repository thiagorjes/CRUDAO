/**
 * Admin API client — CRUD para projeto, workflows, etapas, raias
 */

export interface AdminError {
  error?: string;
  message?: string;
  status?: number;
}

/**
 * Atualizar projeto
 * PUT /api/admin/projeto/{id}
 */
export async function atualizarProjeto(
  projetoId: string,
  dados: { nome?: string; descricao?: string; finalizado?: boolean }
): Promise<any> {
  const res = await fetch(`/api/admin/projeto/${projetoId}`, {
    method: "PUT",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(dados),
  });

  if (!res.ok) {
    const error = (await res.json()) as AdminError;
    throw new Error(error.message || `Erro ${res.status}`);
  }

  return res.json();
}

/**
 * Deletar projeto
 * DELETE /api/admin/projeto/{id}
 */
export async function deletarProjeto(projetoId: string): Promise<void> {
  const res = await fetch(`/api/admin/projeto/${projetoId}`, {
    method: "DELETE",
    credentials: "include",
  });

  if (!res.ok) {
    const error = (await res.json()) as AdminError;
    throw new Error(error.message || `Erro ${res.status}`);
  }
}

/**
 * Criar workflow
 * POST /api/admin/workflows
 */
export async function criarWorkflow(
  projetoId: string,
  dados: { nome: string }
): Promise<any> {
  const res = await fetch("/api/admin/workflows", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ projetoId, ...dados }),
  });

  if (!res.ok) {
    const error = (await res.json()) as AdminError;
    throw new Error(error.message || `Erro ${res.status}`);
  }

  return res.json();
}

/**
 * Obter workflows de projeto
 * GET /api/admin/workflows?projetoId={id}
 */
export async function obterWorkflows(projetoId: string): Promise<any[]> {
  const res = await fetch(`/api/admin/workflows?projetoId=${projetoId}`, {
    method: "GET",
    credentials: "include",
  });

  if (!res.ok) {
    const error = (await res.json()) as AdminError;
    throw new Error(error.message || `Erro ${res.status}`);
  }

  return res.json();
}

/**
 * Criar raia
 * POST /api/admin/raias
 */
export async function criarRaia(
  projetoId: string,
  dados: { nome: string }
): Promise<any> {
  const res = await fetch("/api/admin/raias", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ projetoId, ...dados }),
  });

  if (!res.ok) {
    const error = (await res.json()) as AdminError;
    throw new Error(error.message || `Erro ${res.status}`);
  }

  return res.json();
}

/**
 * Obter raias de projeto
 * GET /api/admin/raias?projetoId={id}
 */
export async function obterRaias(projetoId: string): Promise<any[]> {
  const res = await fetch(`/api/admin/raias?projetoId=${projetoId}`, {
    method: "GET",
    credentials: "include",
  });

  if (!res.ok) {
    const error = (await res.json()) as AdminError;
    throw new Error(error.message || `Erro ${res.status}`);
  }

  return res.json();
}
