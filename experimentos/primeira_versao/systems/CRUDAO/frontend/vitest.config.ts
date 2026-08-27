import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./vitest.setup.ts'],
    // e2e/ são specs do Playwright (TASK-06.1) — suíte e runner separados do vitest.
    exclude: ['**/node_modules/**', 'e2e/**'],
  },
});
