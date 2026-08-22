# TASK-06.1 — Testes E2E dos fluxos principais e revisão de cobertura [M]

**Epic:** EPIC-06 — Testes E2E e Fechamento | **User Story:** US-06.1 — Validação final
**Sistema:** CRUDAO | **RF:** todos (validação cruzada) | **Dependências:** TASK-05.1, TASK-05.2, TASK-05.3

---

## Contexto

Fechar a primeira entrega com testes de ponta a ponta dos fluxos críticos e confirmar a cobertura exigida pelas guidelines (80% TDD / 100% BDD).

## O que deve ser feito

- [ ] Escolher e configurar ferramenta de E2E (Q-005 da techspec, ex. Playwright)
- [ ] Cobrir fluxos: mover tarefa (drag e menu), marcar/desmarcar impedimento, desfinalizar, dashboard assíncrono, RBAC (bloqueio de ação sem permissão)
- [ ] Revisar cobertura de testes unitários/integração contra a meta de guidelines/testing.md
- [ ] Revisar `docs/spdd/kanban-configuravel-canvas.md` — confirmar se todas as dimensões estão preenchidas para transição a `READY`

## Guia técnico

- Referência: `systems/CRUDAO/guidelines/testing.md`

## Critérios de aceite

- Fluxos críticos cobertos por E2E, passando localmente
- Cobertura de testes atinge as metas definidas em guidelines

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
