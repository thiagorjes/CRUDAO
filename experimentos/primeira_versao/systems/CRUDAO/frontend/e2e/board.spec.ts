import { test, expect } from '@playwright/test';
import { ADMIN, ApiCliente, criarCenario, criarTarefa } from './fixtures/api';
import { loginUi } from './fixtures/login';

/**
 * Fluxos críticos do board (RF-001, RF-002, RF-004, RF-005, RF-012) — TASK-06.1.
 * Cada teste cria seu próprio projeto via API para não interferir com os demais.
 */
test.describe('Board', () => {
  async function abrirProjeto(page: import('@playwright/test').Page, projetoId: string, admin: ApiCliente) {
    const nomeProjeto = (await admin.get<{ nome: string }>(`/projetos/${projetoId}`)).nome;
    await page.locator('select').first().selectOption({ label: nomeProjeto });
  }

  test('mover tarefa via menu do card (RF-002)', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Board Menu ${Date.now()}`);
    const tarefa = await criarTarefa(admin, cenario, 'Tarefa via menu');

    await loginUi(page, ADMIN);
    await abrirProjeto(page, cenario.projetoId, admin);

    const card = page.getByTestId('card-tarefa').filter({ hasText: 'Tarefa via menu' });
    await expect(card).toBeVisible();
    await card.getByLabel('Menu de ações da tarefa').click();
    await page.getByTestId('menu-acoes-tarefa').getByRole('button', { name: /Em Andamento/ }).click();

    await expect(page.getByText('Tarefa movida.')).toBeVisible();
    const atualizada = await admin.get<{ etapaAtualId: string }>(`/tarefas/${tarefa.id}`);
    expect(atualizada.etapaAtualId).toBe(cenario.etapas[1].id);
  });

  test('mover tarefa via drag-and-drop (RF-002, DDR-002)', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Board DnD ${Date.now()}`);
    const tarefa = await criarTarefa(admin, cenario, 'Tarefa via drag');

    await loginUi(page, ADMIN);
    await abrirProjeto(page, cenario.projetoId, admin);

    const card = page.getByTestId('card-tarefa').filter({ hasText: 'Tarefa via drag' });
    const colunaDestino = page.getByTestId(`celula-etapa-${cenario.etapas[1].id}`);

    await card.dragTo(colunaDestino);

    await expect(async () => {
      const atualizada = await admin.get<{ etapaAtualId: string }>(`/tarefas/${tarefa.id}`);
      expect(atualizada.etapaAtualId).toBe(cenario.etapas[1].id);
    }).toPass({ timeout: 5000 });
  });

  test('mover para etapa sem transição válida é rejeitado pelo backend (409)', async () => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Board Invalido ${Date.now()}`);
    const tarefa = await criarTarefa(admin, cenario, 'X');
    const status = await admin.status('PATCH', `/tarefas/${tarefa.id}/mover`, {
      etapaDestinoId: cenario.etapas[2].id, // A Fazer -> Concluída direto, sem transição
    });
    expect(status).toBe(409);
  });

  test('marcar e desmarcar impedimento reflete no card e no detalhe (RF-004)', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Impedimento ${Date.now()}`);
    const tarefa = await criarTarefa(admin, cenario, 'Tarefa impedida');

    await loginUi(page, ADMIN);
    await page.goto(`/tarefas/${tarefa.id}`);
    await page.getByRole('button', { name: 'Marcar como impedida' }).click();
    await expect(page.getByRole('button', { name: 'Remover impedimento' })).toBeVisible();

    let atual = await admin.get<{ impedida: boolean }>(`/tarefas/${tarefa.id}`);
    expect(atual.impedida).toBe(true);

    await page.getByRole('button', { name: 'Remover impedimento' }).click();
    await expect(page.getByRole('button', { name: 'Marcar como impedida' })).toBeVisible();
    atual = await admin.get<{ impedida: boolean }>(`/tarefas/${tarefa.id}`);
    expect(atual.impedida).toBe(false);
  });

  test('desfinalizar tarefa via transição de REABERTURA (RF-012)', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Reabertura ${Date.now()}`);
    const tarefa = await criarTarefa(admin, cenario, 'Tarefa finalizada', {
      etapaInicialId: cenario.etapas[2].id,
    });

    await loginUi(page, ADMIN);
    await abrirProjeto(page, cenario.projetoId, admin);

    const card = page.getByTestId('card-tarefa').filter({ hasText: 'Tarefa finalizada' });
    await card.getByLabel('Menu de ações da tarefa').click();
    const botaoReabrir = page
      .getByTestId('menu-acoes-tarefa')
      .getByRole('button', { name: /↺ Reabrir em: Em Andamento/ });
    await expect(botaoReabrir).toBeVisible();
    await botaoReabrir.click();

    await expect(page.getByText('Tarefa movida.')).toBeVisible();
    const atualizada = await admin.get<{ etapaAtualId: string }>(`/tarefas/${tarefa.id}`);
    expect(atualizada.etapaAtualId).toBe(cenario.etapas[1].id);
  });

  test('evento em tempo real (STOMP) reflete movimentação feita por outra sessão em até 2s (RNF-001)', async ({
    browser,
  }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Realtime ${Date.now()}`);
    const tarefa = await criarTarefa(admin, cenario, 'Tarefa tempo real');

    const context = await browser.newContext();
    const page = await context.newPage();
    await loginUi(page, ADMIN);
    await abrirProjeto(page, cenario.projetoId, admin);

    const card = page.getByTestId('card-tarefa').filter({ hasText: 'Tarefa tempo real' });
    await expect(card).toHaveAttribute('data-etapa-atual-id', cenario.etapas[0].id);

    await admin.patch(`/tarefas/${tarefa.id}/mover`, { etapaDestinoId: cenario.etapas[1].id });

    // Sem reload — o card deve refletir a nova etapa via evento STOMP em até 2s (RNF-001).
    await expect(card).toHaveAttribute('data-etapa-atual-id', cenario.etapas[1].id, { timeout: 2500 });

    await context.close();
  });
});
