import { defineConfig, devices } from '@playwright/test';

/**
 * Config de E2E (TASK-06.1, Q-005 da techspec) — roda contra a stack real via `docker compose up`
 * (frontend em :3000, backend em :8080, Keycloak em :8081). Não sobe/derruba a stack automaticamente
 * — pré-condição documentada no README/quickstart.
 */
export default defineConfig({
  testDir: './e2e',
  globalSetup: require.resolve('./e2e/global-setup.ts'),
  fullyParallel: true,
  workers: 3,
  retries: 0,
  reporter: 'list',
  timeout: 30000,
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
