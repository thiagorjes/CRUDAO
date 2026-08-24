import { Page, expect } from '@playwright/test';
import { Credenciais } from './api';

/**
 * Login via UI real (Keycloak Authorization Code) — mesmo fluxo de um usuário (TASK-05.0).
 * Sempre desloga antes: evita reaproveitar a sessão de um login anterior nesta mesma `page`
 * quando um teste troca de usuário no meio do fluxo (ex.: comparar admin vs. user.teste).
 */
export async function loginUi(page: Page, { usuario, senha }: Credenciais, destino = '/') {
  await page.goto('/api/auth/logout').catch(() => undefined);
  await page.goto(destino);
  await page.waitForURL(/realms\/crudao\/protocol\/openid-connect\/auth/);
  await page.fill('#username', usuario);
  await page.fill('#password', senha);
  await page.click('#kc-login');
  await expect(page).toHaveURL(new RegExp(`localhost:3000${destino === '/' ? '/' : destino}`));
}
