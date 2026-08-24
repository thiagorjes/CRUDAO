# TASK-06.1 — Testes E2E dos fluxos principais e revisão de cobertura [M]

**Status:** Concluída — 2026-08-24

**Epic:** EPIC-06 — Testes E2E e Fechamento | **User Story:** US-06.1 — Validação final
**Sistema:** CRUDAO | **RF:** todos (validação cruzada) | **Dependências:** TASK-05.1, TASK-05.2, TASK-05.3, TASK-05.4

---

## Contexto

Fechar a primeira entrega com testes de ponta a ponta dos fluxos críticos e confirmar a cobertura exigida pelas guidelines (80% TDD / 100% BDD).

## O que deve ser feito

- [x] Escolher e configurar ferramenta de E2E (Q-005 da techspec, ex. Playwright) — Playwright, `systems/CRUDAO/frontend/playwright.config.ts`
- [x] Cobrir fluxos: mover tarefa (drag e menu), marcar/desmarcar impedimento, desfinalizar, dashboard assíncrono, RBAC (bloqueio de ação sem permissão)
- [x] Cobrir fluxos novos (PRD v1.2/v1.3): RBAC por projeto (permissão em um projeto não vaza para outro), autoatribuição de tarefa, `tarefa:finalizar`, projeto finalizado bloqueando escrita, toggles de projeto, painel de Papéis visível só a admin global
- [x] Revisar cobertura de testes unitários/integração contra a meta de guidelines/testing.md
- [x] Revisar `docs/spdd/kanban-configuravel-canvas.md` — já estava `READY` (7/7 dimensões, desde 2026-08-23), confirmado sem alteração

## Guia técnico

- Referência: `systems/CRUDAO/guidelines/testing.md`

## Critérios de aceite

- Fluxos críticos cobertos por E2E, passando localmente
- Cobertura de testes atinge as metas definidas em guidelines

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
