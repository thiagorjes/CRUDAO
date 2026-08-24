import { test, expect } from '@playwright/test';
import { ADMIN, USER, ApiCliente, criarCenario, criarTarefa } from './fixtures/api';
import { loginUi } from './fixtures/login';

/**
 * Fluxos de criação e exclusão de card pelo board (RF-001, RF-002) — TASK-03.1.
 * Cada teste cria seu próprio projeto via API para não interferir com os demais.
 * `dev` tem `tarefa:gerenciar` (mas não `tarefa:atribuir`/`tarefa:finalizar`); `gestor` não tem
 * `tarefa:gerenciar` — usados para os cenários de permissão negada.
 */
test.describe('Criação e exclusão de card', () => {
  async function abrirProjeto(page: import('@playwright/test').Page, projetoId: string, admin: ApiCliente) {
    const nomeProjeto = (await admin.get<{ nome: string }>(`/projetos/${projetoId}`)).nome;
    await page.locator('select').first().selectOption({ label: nomeProjeto });
  }

  test('criar card pela UI (RF-001)', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Criar Card ${Date.now()}`);

    await loginUi(page, ADMIN);
    await abrirProjeto(page, cenario.projetoId, admin);

    await page.getByTestId('botao-novo-card').click();
    await page.getByLabel('Título').fill('Card criado pela UI');
    await page.getByRole('button', { name: 'Criar card' }).click();

    await expect(page.getByText('Card criado.')).toBeVisible();
    const card = page.getByTestId('card-tarefa').filter({ hasText: 'Card criado pela UI' });
    await expect(card).toBeVisible();
    await expect(card).toHaveAttribute('data-etapa-atual-id', cenario.etapas[0].id);
  });

  test('usuário sem tarefa:gerenciar não vê o botão "Novo card"', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Sem Permissao Criar ${Date.now()}`, ['gestor']);

    await loginUi(page, USER);
    await abrirProjeto(page, cenario.projetoId, admin);

    await expect(page.getByTestId('botao-novo-card')).toHaveCount(0);
  });

  test('criação bloqueada em projeto finalizado (erro exibido, card não criado)', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Criar Finalizado ${Date.now()}`);
    await admin.put(`/projetos/${cenario.projetoId}/finalizar`);

    await loginUi(page, ADMIN);
    await abrirProjeto(page, cenario.projetoId, admin);

    await page.getByTestId('botao-novo-card').click();
    await page.getByLabel('Título').fill('Card em projeto finalizado');
    await page.getByRole('button', { name: 'Criar card' }).click();

    await expect(page.getByText('Não foi possível concluir')).toBeVisible();
    const tarefas = await admin.get<unknown[]>(`/tarefas?projetoId=${cenario.projetoId}`);
    expect(tarefas).toHaveLength(0);
  });

  test('excluir card pela UI (RF-002)', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Excluir Card ${Date.now()}`);
    await criarTarefa(admin, cenario, 'Card a excluir');

    await loginUi(page, ADMIN);
    await abrirProjeto(page, cenario.projetoId, admin);

    const card = page.getByTestId('card-tarefa').filter({ hasText: 'Card a excluir' });
    await card.getByTestId('botao-excluir-tarefa').click();
    await page.getByRole('dialog').getByRole('button', { name: 'Excluir', exact: true }).click();

    await expect(page.getByText('Card excluído.')).toBeVisible();
    await expect(card).toHaveCount(0);
  });

  test('usuário sem permissão (dev-tier com toggle desabilitado) não vê o ícone de lixeira', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Sem Permissao Excluir ${Date.now()}`, ['dev']);
    await admin.put(`/projetos/${cenario.projetoId}/configuracao`, {
      devPodeExcluirTarefa: false,
      devPodeEditarTarefaIniciada: true,
      gestorVeBoard: false,
    });
    await criarTarefa(admin, cenario, 'Card protegido');

    await loginUi(page, USER);
    await abrirProjeto(page, cenario.projetoId, admin);

    const card = page.getByTestId('card-tarefa').filter({ hasText: 'Card protegido' });
    await expect(card).toBeVisible();
    await expect(card.getByTestId('botao-excluir-tarefa')).toHaveCount(0);
  });

  test('exclusão propaga em tempo real para um segundo cliente em até 2s (RNF-001)', async ({ browser }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Excluir Tempo Real ${Date.now()}`);
    const tarefa = await criarTarefa(admin, cenario, 'Card tempo real');

    const context = await browser.newContext();
    const page = await context.newPage();
    await loginUi(page, ADMIN);
    await abrirProjeto(page, cenario.projetoId, admin);

    const card = page.getByTestId('card-tarefa').filter({ hasText: 'Card tempo real' });
    await expect(card).toBeVisible();

    await admin.delete(`/tarefas/${tarefa.id}`);

    // Sem reload — o card deve sumir via evento STOMP em até 2s (RNF-001).
    await expect(card).toHaveCount(0, { timeout: 2500 });

    await context.close();
  });

  test('workflow ativo sem etapas → botão "Novo card" desabilitado (D-04)', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const projeto = await admin.post<{ id: string }>('/projetos', { nome: `Sem Etapas ${Date.now()}`, descricao: null });
    const workflow = await admin.post<{ id: string }>('/workflows', { projetoId: projeto.id, nome: 'Vazio' });
    await admin.put(`/projetos/${projeto.id}/workflow-ativo`, { workflowId: workflow.id });

    await loginUi(page, ADMIN);
    await abrirProjeto(page, projeto.id, admin);

    await expect(page.getByTestId('botao-novo-card')).toBeDisabled();
  });

  test('fluxo combinado: criar um card e em seguida excluí-lo na mesma sessão', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Criar E Excluir ${Date.now()}`);

    await loginUi(page, ADMIN);
    await abrirProjeto(page, cenario.projetoId, admin);

    await page.getByTestId('botao-novo-card').click();
    await page.getByLabel('Título').fill('Card efêmero');
    await page.getByRole('button', { name: 'Criar card' }).click();
    await expect(page.getByText('Card criado.')).toBeVisible();

    const card = page.getByTestId('card-tarefa').filter({ hasText: 'Card efêmero' });
    await expect(card).toBeVisible();

    await card.getByTestId('botao-excluir-tarefa').click();
    await page.getByRole('dialog').getByRole('button', { name: 'Excluir', exact: true }).click();
    await expect(page.getByText('Card excluído.')).toBeVisible();
    await expect(card).toHaveCount(0);
  });
});
