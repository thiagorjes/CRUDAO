# Estado Operacional — CRUDAO
_Atualizado em: 2026-08-22_

> Estado atual do workspace e das features em andamento.
> Para princípios estáveis e ADRs, veja [memory/constitution.md](constitution.md).

---

## Toolset

**Versão:** 2026-08-22 — atualize com `scripts/update.ps1`
**Pipeline SSPDD:** /guidelines → /prd → [/clarify] → [/checklist] → /techspec → /spdd-canvas → /tasks → [/analyze] → /implement → [/spdd-sync] → /code-review

---

## Sistemas

| Sistema | Caminho | Cenário | Guidelines | Observações |
|---|---|---|---|---|
| CRUDAO | `systems/CRUDAO/` | Novo (greenfield) | ok | Guidelines gerados em 2026-08-22 |

---

## Features Ativas

| Feature | Sistemas afetados | PRD | TechSpec | Tasks | Status |
|---|---|---|---|---|---|
| kanban-configuravel | CRUDAO | 1.1 | 1.0 | 1.0 | Pronto para implementação (/implement TASK-00.1) |

---

## Artifact Registry

| Artefato | v | Status |
|---|---|---|
| docs/discovery/kanban-configuravel-discovery.md | 1.0 | ok |
| docs/prd/kanban-configuravel-prd.md | 1.1 | ok |
| docs/techspec/kanban-configuravel-techspec.md | 1.0 | ok |
| docs/techspec/kanban-configuravel/data-model.md | 1.0 | ok |
| docs/contracts/CRUDAO-keycloak-mock-contract.md | 0.1 | pendente de validação |
| docs/design/kanban-configuravel-design-brief.md | 1.0 | ok |
| docs/design/prototypes/kanban-configuravel/ (fontes + Artifact https://claude.ai/code/artifact/a7612319-88d0-434d-9729-64d3d1604c6c) | — | aprovado pelo usuário em 2026-08-22 |
| docs/tasks/kanban-configuravel-tasks.md (índice) | 1.0 | ok |
| docs/tasks/kanban-configuravel/ (12 arquivos TASK-*.md) | 1.0 | ok |
| docs/spdd/kanban-configuravel-canvas.md | — | draft (R, E, A, S, N, O preenchidas; falta Safeguards — aguarda /code-review) |
| systems/CRUDAO/guidelines/stack.md | 1.0 | ok |
| systems/CRUDAO/guidelines/architecture.md | 1.0 | ok |
| systems/CRUDAO/guidelines/coding-standards.md | 1.0 | ok |
| systems/CRUDAO/guidelines/testing.md | 1.0 | ok |
| systems/CRUDAO/guidelines/security.md | 1.0 | ok |
| systems/CRUDAO/guidelines/observability.md | 1.0 | ok |
| systems/CRUDAO/guidelines/git-workflow.md | 1.0 | ok |
| systems/CRUDAO/guidelines/skill-conventions.md | 1.0 | ok |
| systems/CRUDAO/guidelines/spdd-integration.md | 1.0 | ok |

---

## Evolução do SDD

| Data | Mudança |
|---|---|
| 2026-08-22 | Workspace inicializado via init.py |
