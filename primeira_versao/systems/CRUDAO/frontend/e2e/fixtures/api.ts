/**
 * Helpers de setup via API para os testes E2E (TASK-06.1) — cria os cenários (projeto, workflow,
 * etapas, transições, raias, tarefas, membros) diretamente pela API do backend, autenticado via
 * Keycloak (password grant, `directAccessGrantsEnabled` no realm de dev), em vez de depender da UI
 * de administração para montar a fixture de cada teste. A UI é exercitada só nos fluxos que o
 * critério de aceite da task pede (board, tarefa, dashboard, admin).
 */
const KEYCLOAK_URL = 'http://localhost:8081/realms/crudao/protocol/openid-connect/token';
const API_URL = 'http://localhost:8080/api';

export type Credenciais = { usuario: string; senha: string };

export const ADMIN: Credenciais = { usuario: 'admin.teste', senha: 'admin123' };
export const USER: Credenciais = { usuario: 'user.teste', senha: 'user123' };

async function obterToken({ usuario, senha }: Credenciais): Promise<string> {
  const resp = await fetch(KEYCLOAK_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'password',
      client_id: 'crudao-app',
      client_secret: 'crudao-app-secret-dev',
      username: usuario,
      password: senha,
    }),
  });
  if (!resp.ok) throw new Error(`Login Keycloak falhou para ${usuario}: ${resp.status}`);
  const json = (await resp.json()) as { access_token: string };
  return json.access_token;
}

export class ApiCliente {
  private constructor(private readonly token: string) {}

  static async autenticar(cred: Credenciais): Promise<ApiCliente> {
    return new ApiCliente(await obterToken(cred));
  }

  private async chamar<T>(metodo: string, path: string, body?: unknown): Promise<T> {
    const resp = await fetch(`${API_URL}${path}`, {
      method: metodo,
      headers: {
        Authorization: `Bearer ${this.token}`,
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    if (!resp.ok) {
      const texto = await resp.text().catch(() => '');
      throw new Error(`${metodo} ${path} -> ${resp.status}: ${texto}`);
    }
    if (resp.status === 204) return undefined as T;
    const texto = await resp.text();
    return (texto ? JSON.parse(texto) : undefined) as T;
  }

  get<T>(path: string) {
    return this.chamar<T>('GET', path);
  }
  post<T>(path: string, body?: unknown) {
    return this.chamar<T>('POST', path, body ?? {});
  }
  put<T>(path: string, body?: unknown) {
    return this.chamar<T>('PUT', path, body ?? {});
  }
  patch<T>(path: string, body?: unknown) {
    return this.chamar<T>('PATCH', path, body ?? {});
  }
  delete<T>(path: string) {
    return this.chamar<T>('DELETE', path);
  }

  /** Chamada que espera falhar (usada para assertar 401/403/409) — não lança, retorna o status. */
  async status(metodo: string, path: string, body?: unknown): Promise<number> {
    const resp = await fetch(`${API_URL}${path}`, {
      method: metodo,
      headers: {
        Authorization: `Bearer ${this.token}`,
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    return resp.status;
  }
}

type Etapa = { id: string; nome: string; ordem: number; etapaFinal: boolean };
type Cenario = {
  projetoId: string;
  workflowId: string;
  etapas: Etapa[]; // ordenadas
  raiaId: string;
};

/**
 * Cria um projeto novo com workflow ativo de 3 etapas (A Fazer → Em Andamento → Concluída) e
 * transições de avanço/retrocesso + reabertura (Concluída → Em Andamento), mais uma raia e,
 * opcionalmente, papéis do `user.teste` nesse projeto. Cada teste cria seu próprio projeto para
 * não interferir com os demais.
 */
export async function criarCenario(
  admin: ApiCliente,
  nomeProjeto: string,
  papeisUserTeste: string[] = [],
): Promise<Cenario> {
  const projeto = await admin.post<{ id: string }>('/projetos', { nome: nomeProjeto, descricao: null });
  const workflow = await admin.post<{ id: string }>('/workflows', { projetoId: projeto.id, nome: 'Padrão' });
  const aFazer = await admin.post<Etapa>('/etapas', {
    workflowId: workflow.id,
    nome: 'A Fazer',
    ordem: 0,
    etapaFinal: false,
  });
  const emAndamento = await admin.post<Etapa>('/etapas', {
    workflowId: workflow.id,
    nome: 'Em Andamento',
    ordem: 1,
    etapaFinal: false,
  });
  const concluida = await admin.post<Etapa>('/etapas', {
    workflowId: workflow.id,
    nome: 'Concluída',
    ordem: 2,
    etapaFinal: true,
  });
  await admin.post('/transicoes', { etapaOrigemId: aFazer.id, etapaDestinoId: emAndamento.id, tipo: 'NORMAL' });
  await admin.post('/transicoes', { etapaOrigemId: emAndamento.id, etapaDestinoId: concluida.id, tipo: 'NORMAL' });
  await admin.post('/transicoes', {
    etapaOrigemId: emAndamento.id,
    etapaDestinoId: aFazer.id,
    tipo: 'NORMAL',
  });
  await admin.post('/transicoes', {
    etapaOrigemId: concluida.id,
    etapaDestinoId: emAndamento.id,
    tipo: 'REABERTURA',
  });
  await admin.put(`/projetos/${projeto.id}/workflow-ativo`, { workflowId: workflow.id });
  const raia = await admin.post<{ id: string }>('/raias', { projetoId: projeto.id, nome: 'Raia Única', ordem: 0 });

  if (papeisUserTeste.length > 0) {
    const papeis = await admin.get<{ id: string; nome: string }[]>('/papeis');
    const userMe = await ApiCliente.autenticar(USER).then((c) => c.get<{ id: string }>('/usuarios/me'));
    const papeisIds = papeisUserTeste.map((nome) => {
      const p = papeis.find((x) => x.nome === nome);
      if (!p) throw new Error(`Papel '${nome}' não encontrado no catálogo`);
      return p.id;
    });
    await admin.put(`/projetos/${projeto.id}/membros/${userMe.id}`, { papeis: papeisIds });
  }

  return {
    projetoId: projeto.id,
    workflowId: workflow.id,
    etapas: [aFazer, emAndamento, concluida],
    raiaId: raia.id,
  };
}

export async function criarTarefa(
  admin: ApiCliente,
  cenario: Cenario,
  titulo: string,
  opts?: { etapaInicialId?: string; responsavelId?: string },
): Promise<{ id: string; titulo: string }> {
  return admin.post('/tarefas', {
    projetoId: cenario.projetoId,
    etapaInicialId: opts?.etapaInicialId ?? cenario.etapas[0].id,
    raiaId: cenario.raiaId,
    tipo: 'FEATURE',
    titulo,
    descricao: null,
    responsavelId: opts?.responsavelId ?? null,
  });
}
