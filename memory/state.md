# Estado Operacional — CRUDAO
_Atualizado em: 2026-08-24_

> Estado atual do workspace e das features em andamento.
> Para princípios estáveis e ADRs, veja [memory/constitution.md](constitution.md).

---

## Toolset

**Versão:** 2026-08-24 — atualize com `scripts/update.py`
**Pipeline SSPDD:** /guidelines → /discovery → /prd → [/clarify] → [/checklist] → [/designer] → /techspec → /tasks → [/analyze] → /implement ou /tdd → /code-review → /tests → [/spdd-sync]

---

## Sistemas

| Sistema | Caminho | Cenário | Guidelines | Observações |
|---|---|---|---|---|
| CRUDAO | `systems/CRUDAO/` | Novo (greenfield) | pendente | — |

---

## Features Ativas

| Feature | Sistemas afetados | PRD | TechSpec | Tasks | Status |
|---|---|---|---|---|---|
| kanban-tarefas | CRUDAO | 1.0 | 1.0 | — | Em especificação técnica → pronto para /tasks |

---

## Artifact Registry

| Artefato | v | Status |
|---|---|---|
| docs/discovery/kanban-tarefas-discovery.md | 1.0 | ok |
| docs/prd/kanban-tarefas-prd.md | 1.0 | ok |
| docs/design/kanban-tarefas-design-brief.md | 1.0 | ok |
| docs/techspec/kanban-tarefas-techspec.md | 1.0 | ok |
| docs/techspec/kanban-tarefas/data-model.md | 1.0 | ok |
| docs/techspec/kanban-tarefas/quickstart.md | 1.0 | ok |
| docs/decisions/ADR-004-broadcast-listen-notify.md | 1.0 | ok |
| docs/decisions/ADR-005-flyway-migrations.md | 1.0 | ok |
| docs/decisions/ADR-006-sem-fallback-auth-keycloak.md | 1.0 | ok |
| docs/spdd/kanban-tarefas-canvas.md | — | draft (6/7 dimensões — aguarda /tasks) |

---

## Evolução do SDD

| Data | Mudança |
|---|---|
| 2026-08-24 | Workspace inicializado via init.py |
| 2026-08-25 | /prd kanban-tarefas concluído (v1.0) |
| 2026-08-25 | /designer kanban-tarefas concluído (v1.0) |
| 2026-08-25 | /techspec kanban-tarefas concluído (v1.0) — ADR-004/005/006 criados |

---

## kanban-tarefas

- **Etapa concluída:** /techspec (v1.0) — 2026-08-25
- **Artefatos:** docs/techspec/kanban-tarefas-techspec.md + data-model.md + quickstart.md + 7 contratos de API
- **Sistemas afetados:** CRUDAO
- **Mock contracts:** nenhum (Keycloak e PostgreSQL sem necessidade de mock)
- **ADRs criados:** ADR-004 (broadcast LISTEN/NOTIFY, atualizado pós-comitê), ADR-005 (Flyway), ADR-006 (sem fallback auth Keycloak)
- **Comitê de Análise Assíncrono:** executado 2026-08-25 (Architect, Security, Database, DevOps, QA/general-purpose) — 13 achados aplicados aos artefatos salvos: `EventoBoardPublisher` (porta de domínio + afterCommit), resincronização client-side por `seq`, autorização em subscrição STOMP, RN-017 (bloqueia autoconcessão de permissão), índices em `TarefaEtapaHistorico`/`TarefaImpedimentoHistorico`, exigência de projeção DTO (evitar N+1), health-checks (listener + Keycloak) e métricas mínimas via Actuator, logout com back-channel Keycloak, checagem de `Usuario.ativo`, cobertura Gherkin dos 19 RFs na Seção 7
- **Decisão em aberto:** densidade do card no board (compacto vs. expandido, TL-03/TL-03b) — segue sem resolução, não bloqueia /tasks
- **Nota:** ADR-001/002/003 referenciados nas guidelines mas ausentes em docs/decisions/ (pré-existente) — recriar antes de auditoria formal
- **Próximo comando:** /tasks kanban-tarefas
