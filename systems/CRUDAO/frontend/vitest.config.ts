import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import path from "node:path";

export default defineConfig({
  // tsconfig.json usa jsx:"preserve" (Next.js transforma via SWC no build real) — o plugin do
  // React garante que o JSX dos testes de componente seja transformado corretamente pelo Vitest.
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    include: ["**/*.test.ts", "**/*.test.tsx"],
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "."),
    },
  },
});
