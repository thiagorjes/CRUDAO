# TASK-03.1 — Testes E2E de criação e exclusão de card [P]

**Epic:** EPIC-03 — Testes E2E
**Sistema:** CRUDAO | **RF:** RF-001, RF-002 | **Dependências:** TASK-02.2, TASK-02.3

---

## Contexto

Cobertura E2E de ponta a ponta dos fluxos de criação e exclusão de card, contra a stack real (`docker compose up`), seguindo o padrão já configurado na TASK-06.1 (kanban-configuravel) — Playwright, fixtures via API autenticada por Keycloak, `data-testid` para seletores estáveis.

## O que deve ser feito

- [ ] Novo spec `e2e/criacao-exclusao-card.spec.ts` (ou extensão de `e2e/board.spec.ts`, avaliar no momento da implementação qual organização fica mais coesa), seguindo o padrão de fixtures existente (`e2e/fixtures/api.ts`, `e2e/fixtures/login.ts`, `e2e/global-setup.ts`)
- [ ] Cenário: usuário com `tarefa:gerenciar` cria card pela UI (botão "Novo card" → preenche formulário → salva) → card aparece no board na etapa/raia padrão esperada
- [ ] Cenário: usuário sem `tarefa:gerenciar` no projeto não vê o botão "Novo card"
- [ ] Cenário: criação bloqueada em projeto finalizado (erro exibido, card não criado)
- [ ] Cenário: usuário com `tarefa:gerenciar` exclui card pela UI (ícone de lixeira → confirma no modal) → card desaparece do board
- [ ] Cenário: usuário sem permissão (ou `dev`-tier com toggle `devPodeExcluirTarefa` desabilitado) não vê o ícone de lixeira
- [ ] Cenário: exclusão propaga em tempo real para um segundo cliente conectado ao mesmo projeto (2 contextos de browser/sessão do Playwright, mesmo padrão dos testes de tempo real já existentes em `board.spec.ts`), em até 2s (RNF-001)
- [ ] Cenário: workflow ativo sem etapas configuradas → botão "Novo card" desabilitado (cobertura E2E do trade-off D-04, além do teste unitário já previsto na TASK-02.2)
- [ ] Cenário combinado: criar um card pela UI e em seguida excluí-lo na mesma sessão (fluxo real mais comum do usuário, único cenário que exercita `ModalNovoCard` e o modal de confirmação de exclusão interagindo com o mesmo estado do `BoardApp`)
- [ ] Adicionar `data-testid` novos em `ModalNovoCard`/ícone de lixeira/modal de confirmação, se necessário para seletores estáveis (mesmo padrão da TASK-06.1)

## Guia técnico

- Arquivos a criar/modificar (paths relativos a `systems/CRUDAO/`):
  - `frontend/e2e/criacao-exclusao-card.spec.ts` (novo) ou `frontend/e2e/board.spec.ts` (estendido)
  - Componentes de `EPIC-02`, se precisarem de `data-testid` adicional
- `frontend/playwright.config.ts` já tem `globalSetup` provisionando `admin.teste`/`user.teste` — reaproveitar sem alteração
- Specs rodam contra `docker compose up` real — não sobem/derrubam a stack sozinhas (mesmo padrão da TASK-06.1); cada teste cria seu próprio projeto via API para evitar interferência entre testes paralelos

## Critérios de aceite

- Todos os 8 cenários listados acima passando contra a stack real
- Suíte E2E completa (specs desta feature + specs de kanban-configuravel já existentes) revalidada sem regressão
- `tsc --noEmit`, `eslint`, `next build` do frontend seguem limpos após as mudanças

---

**Status:** Não iniciada

---

_Origem: [docs/tasks/criacao-card-board-tasks.md](../criacao-card-board-tasks.md)_
