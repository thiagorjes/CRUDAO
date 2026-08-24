import { test, expect } from '@playwright/test';
import { ADMIN, USER, ApiCliente, criarCenario, criarTarefa } from './fixtures/api';
import { loginUi } from './fixtures/login';

/**
 * RBAC por projeto (RF-013 a RF-017, BDR-001, ADR-006) — TASK-06.1.
 * `user.teste` é usado como usuário não-admin, recebendo papéis diferentes por projeto via API.
 */
test.describe('RBAC', () => {
  test('bloqueia ação sem permissão no projeto (dev não pode editar dados do projeto)', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `RBAC Bloqueio ${Date.now()}`, ['dev']);
    const nomeProjeto = (await admin.get<{ nome: string }>(`/projetos/${cenario.projetoId}`)).nome;

    await loginUi(page, USER);
    await page.goto('/admin');
    await page.locator('select').first().selectOption({ label: nomeProjeto });

    // "dev" não tem projeto:gerenciar — campos de edição do projeto ficam desabilitados e não há
    // botão de finalizar; a aba "Membros"/"Configurações" fica oculta (só projeto:gerenciar vê).
    await expect(page.locator('#proj-nome')).toBeDisabled();
    await expect(page.getByRole('button', { name: /Finalizar projeto/ })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Membros' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Configurações' })).toHaveCount(0);

    // Reforço via API direta — backend é a fonte real de autorização (RNF-003).
    const userApi = await ApiCliente.autenticar(USER);
    const status = await userApi.status('PUT', `/projetos/${cenario.projetoId}`, {
      nome: 'Tentativa não autorizada',
      descricao: null,
    });
    expect(status).toBe(403);
  });

  test('permissão concedida em um projeto não vaza para outro (isolamento por projeto)', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenarioA = await criarCenario(admin, `RBAC Isolamento A ${Date.now()}`, ['project_admin']);
    const cenarioB = await criarCenario(admin, `RBAC Isolamento B ${Date.now()}`); // sem papel para user.teste

    const userApi = await ApiCliente.autenticar(USER);
    const statusA = await userApi.status('PUT', `/projetos/${cenarioA.projetoId}`, {
      nome: 'Editado A',
      descricao: null,
    });
    const statusB = await userApi.status('PUT', `/projetos/${cenarioB.projetoId}`, {
      nome: 'Editado B',
      descricao: null,
    });
    expect(statusA).toBe(200);
    expect(statusB).toBe(403);

    // No painel, o projeto B nem aparece no seletor de "user.teste" (não é membro).
    await loginUi(page, USER);
    await page.goto('/admin');
    const nomeProjetoB = (await admin.get<{ nome: string }>(`/projetos/${cenarioB.projetoId}`)).nome;
    await expect(page.locator('select').first().locator('option', { hasText: nomeProjetoB })).toHaveCount(0);
  });

  test('autoatribuição de tarefa ("Atribuir a mim", RN-012)', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Autoatribuicao ${Date.now()}`, ['dev']);
    const tarefa = await criarTarefa(admin, cenario, 'Tarefa sem responsável');

    await loginUi(page, USER);
    await page.goto(`/tarefas/${tarefa.id}`);
    await page.getByRole('button', { name: 'Atribuir a mim' }).click();

    await expect(page.getByText('Responsável atualizado.')).toBeVisible();
    const userMe = await ApiCliente.autenticar(USER).then((c) => c.get<{ id: string }>('/usuarios/me'));
    const atualizada = await admin.get<{ responsavelId: string }>(`/tarefas/${tarefa.id}`);
    expect(atualizada.responsavelId).toBe(userMe.id);
  });

  test('tarefa:finalizar é exigida na ida e na volta da etapa final (RN-011)', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Finalizar ${Date.now()}`, ['dev']); // dev não tem tarefa:finalizar
    const tarefa = await criarTarefa(admin, cenario, 'Tarefa dev', {
      etapaInicialId: cenario.etapas[1].id,
    });

    const userApi = await ApiCliente.autenticar(USER);
    // Ida: dev tentando mover para a etapa final é rejeitado.
    const statusIda = await userApi.status('PATCH', `/tarefas/${tarefa.id}/mover`, {
      etapaDestinoId: cenario.etapas[2].id,
    });
    expect(statusIda).toBe(403);

    // Admin finaliza a tarefa; dev tentando reabrir (volta) também é rejeitado.
    await admin.patch(`/tarefas/${tarefa.id}/mover`, { etapaDestinoId: cenario.etapas[2].id });
    const statusVolta = await userApi.status('PATCH', `/tarefas/${tarefa.id}/mover`, {
      etapaDestinoId: cenario.etapas[1].id,
    });
    expect(statusVolta).toBe(403);

    // UI: menu do card de dev não deve nem oferecer a ação de reabertura.
    await loginUi(page, USER);
    const nomeProjeto = (await admin.get<{ nome: string }>(`/projetos/${cenario.projetoId}`)).nome;
    await page.goto('/');
    await page.locator('select').first().selectOption({ label: nomeProjeto });
    const card = page.getByTestId('card-tarefa').filter({ hasText: 'Tarefa dev' });
    await card.getByLabel('Menu de ações da tarefa').click();
    await page
      .getByTestId('menu-acoes-tarefa')
      .getByRole('button', { name: /↺ Reabrir em: Em Andamento/ })
      .click();
    // Ação tentada via UI (menu não é gated no frontend) — backend rejeita e mostra erro.
    await expect(page.getByRole('dialog')).toBeVisible();
  });

  test('projeto finalizado bloqueia escrita (RN-015)', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Finalizado ${Date.now()}`);
    const tarefa = await criarTarefa(admin, cenario, 'Tarefa em projeto finalizado');
    await admin.put(`/projetos/${cenario.projetoId}/finalizar`);

    const statusMover = await admin.status('PATCH', `/tarefas/${tarefa.id}/mover`, {
      etapaDestinoId: cenario.etapas[1].id,
    });
    expect(statusMover).toBe(409);

    await loginUi(page, ADMIN);
    await page.goto('/admin');
    const nomeProjeto = (await admin.get<{ nome: string }>(`/projetos/${cenario.projetoId}`)).nome;
    await page.locator('select').first().selectOption({ label: nomeProjeto });
    await expect(page.getByText(/somente leitura/)).toBeVisible();
    await expect(page.getByRole('button', { name: 'Reabrir projeto' })).toBeVisible();
  });

  test('toggles de projeto (RF-016) — devPodeEditarTarefaIniciada trava edição de dev', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Toggles ${Date.now()}`, ['dev']);
    await admin.put(`/projetos/${cenario.projetoId}/configuracao`, {
      devPodeExcluirTarefa: false,
      devPodeEditarTarefaIniciada: false,
      gestorVeBoard: false,
    });
    // Tarefa iniciada: cria na 1ª etapa e move para a 2ª (marca `iniciada=true`).
    const tarefa = await criarTarefa(admin, cenario, 'Tarefa iniciada');
    await admin.patch(`/tarefas/${tarefa.id}/mover`, { etapaDestinoId: cenario.etapas[1].id });

    await loginUi(page, USER);
    await page.goto(`/tarefas/${tarefa.id}`);
    await page.getByRole('button', { name: '🔒 Editar' }).click();
    await expect(page.getByText('edição travada para o seu papel')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Salvar' })).toHaveCount(0);
  });

  test('aba "Papéis e permissões" só é visível para admin global (RF-015)', async ({ page }) => {
    const admin = await ApiCliente.autenticar(ADMIN);
    const cenario = await criarCenario(admin, `Papeis ${Date.now()}`, ['project_admin']);
    const nomeProjeto = (await admin.get<{ nome: string }>(`/projetos/${cenario.projetoId}`)).nome;

    await loginUi(page, ADMIN);
    await page.goto('/admin');
    await expect(page.getByRole('button', { name: 'Papéis e permissões' })).toBeVisible();

    await loginUi(page, USER);
    await page.goto('/admin');
    await page.locator('select').first().selectOption({ label: nomeProjeto });
    // Mesmo com project_admin (sem papel:gerenciar), a aba não aparece — G-RBAC-07.
    await expect(page.getByRole('button', { name: 'Papéis e permissões' })).toHaveCount(0);
    const status = await (await ApiCliente.autenticar(USER)).status('GET', '/papeis');
    expect(status).toBe(200); // leitura não é restrita — só a atribuição via UI/endpoint dedicado
  });
});
