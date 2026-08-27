import { test, expect } from '@playwright/test';
import { ADMIN, ApiCliente, criarCenario, criarTarefa } from './fixtures/api';
import { loginUi } from './fixtures/login';

/** Dashboard assíncrono (RF-006, RF-007) — TASK-06.1. */
test('calcula dashboard assíncrono e exibe lead-time por etapa', async ({ page }) => {
  const admin = await ApiCliente.autenticar(ADMIN);
  const cenario = await criarCenario(admin, `Dashboard ${Date.now()}`);
  const tarefa = await criarTarefa(admin, cenario, 'Tarefa dashboard');
  // Gera algum lead-time real: move a tarefa antes de calcular.
  await admin.patch(`/tarefas/${tarefa.id}/mover`, { etapaDestinoId: cenario.etapas[1].id });
  const nomeProjeto = (await admin.get<{ nome: string }>(`/projetos/${cenario.projetoId}`)).nome;

  await loginUi(page, ADMIN);
  await page.goto('/dashboard');
  await page.locator('select').first().selectOption({ label: nomeProjeto });

  const hoje = new Date().toISOString().slice(0, 10);
  const inicios = page.locator('input[type="date"]');
  await inicios.nth(0).fill('2020-01-01');
  await inicios.nth(1).fill(hoje);

  await page.getByRole('button', { name: /Atualizar|Calculando/ }).click();
  await expect(page.getByRole('columnheader', { name: 'Lead-time médio' })).toBeVisible({ timeout: 10000 });
  await expect(page.getByRole('cell', { name: 'A Fazer' })).toBeVisible();
  await expect(page.getByRole('cell', { name: 'Em Andamento' })).toBeVisible();
});
