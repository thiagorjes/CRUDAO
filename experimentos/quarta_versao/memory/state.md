# Estado Operacional - CRUDAO
_Atualizado em: 2026-08-27_

> Estado atual do workspace e das features em andamento.
> Para principios estaveis e ADRs, veja [memory/constitution.md](constitution.md).

---

## Toolset

**Versao:** 2026-08-27

**Pipeline SSPDD:** /guidelines -> /discovery -> /prd -> [/clarify] -> [/checklist] -> [/designer] -> /techspec -> /tasks -> [/analyze] -> /implement ou /tdd -> /code-review -> /tests -> [/spdd-sync]

---

## Sistemas

| Sistema | Caminho | Cenario | Guidelines | Observacoes |
|---|---|---|---|---|
| CRUDAO | `systems/CRUDAO/` | Novo (greenfield) | ok | Backend Spring Boot + frontend Next.js |

## Features Ativas

| Feature | Sistemas afetados | PRD | TechSpec | Tasks | Status |
|---|---|---|---|---|---|
| kanban-tarefas | CRUDAO | 1.0 | 1.1 | 1.1 | Pronto para implementação |

## Artifact Registry

| Artefato | Versao | Status |
|---|---|---|
| docs/discovery/kanban-tarefas-discovery.md | 1.0 | ok |
| docs/prd/kanban-tarefas-prd.md | 1.0 | ok |
| docs/design/kanban-tarefas-design-brief.md | 1.0 | ok |
| docs/techspec/kanban-tarefas-techspec.md | 1.2 | ok - Docker obrigatorio para todos os componentes |
| docs/techspec/kanban-tarefas/data-model.md | 1.0 | ok |
| docs/techspec/kanban-tarefas/quickstart.md | 1.0 | ok |
| docs/decisions/ADR-004-broadcast-listen-notify.md | 1.0 | ok |
| docs/decisions/ADR-005-flyway-migrations.md | 1.0 | ok |
| docs/decisions/ADR-006-sem-fallback-auth-keycloak.md | 1.0 | ok |
| docs/decisions/ADR-007-bootstrap-admin-global.md | 1.0 | ok |
| docs/decisions/ADR-008-dockerizacao-backend-frontend.md | 1.0 | ok |
| docs/decisions/ADR-001-stack-backend-java-spring.md | 1.0 | ok |
| docs/decisions/ADR-002-postgresql-sem-cache-tempo-real.md | 1.0 | ok |
| docs/decisions/ADR-003-rbac-hibrido-keycloak.md | 1.0 | ok |
| docs/tasks/kanban-tarefas-tasks.md | 1.2 | ok - execução Docker obrigatória |
| docs/checklists/kanban-tarefas-safeguards.md | 1.0 | draft - candidatos extraidos, aguardando /code-review |
| docs/checklists/kanban-tarefas-TASK-01.1-review.md | 1.3 | aprovado com validação runtime pendente |
| docs/spdd/kanban-tarefas-canvas.md | — | draft - Safeguards aguardando /code-review |

> ADR-001, ADR-002 e ADR-003 foram reconstruidos a partir dos arquivos da primeira versao e das referencias posteriores. ADR-004/006/007/008 registram os refinamentos adotados depois.

## Evolucao do SDD

| Data | Mudanca |
|---|---|
| 2026-08-27 | Estado e constituicao reconstruidos para retomada do pipeline |
| 2026-08-27 | PRD, Design Brief e TechSpec 1.1 conferidos como entradas de /tasks |
| 2026-08-27 | /tasks kanban-tarefas atualizado para TechSpec 1.2: Docker obrigatório em todas as tasks; TASK-08.3 reaberta para validação integral |
| 2026-08-27 | Inventario de safeguards extraido dos guidelines e artefatos; validacao de codigo pendente |
| 2026-08-27 | /implement TASK-01.1 concluido: esqueleto backend/frontend, Compose, realm Keycloak e Dockerfiles validados |
| 2026-08-27 | /code-review TASK-01.1 reprovado: Compose não inclui backend/frontend e application-dev usa localhost |
| 2026-08-27 | /techspec v1.2 concluido: Docker tornou-se o unico modo suportado para backend, frontend, Keycloak e PostgreSQL |
| 2026-08-27 | /implement TASK-08.3 concluido: Compose integral, rede Docker, builds e smoke tests ponta a ponta validados |
| 2026-08-28 | /code-review TASK-01.1: C1/I1 resolvidos; validação runtime atual pendente por Docker Desktop indisponível |

## kanban-tarefas

- **Etapa concluida:** /techspec v1.2
- **Entradas aprovadas:** PRD v1.0, TechSpec v1.2, data model, contratos, quickstart e guidelines do sistema
- **Etapa concluida:** /tasks v1.2 — 2026-08-27
- **Artefato:** docs/tasks/kanban-tarefas-tasks.md
- **Total de tasks:** 26 tasks em 8 epics
- **Canvas:** DRAFT (Safeguards aguarda /code-review)
- **Safeguards:** docs/checklists/kanban-tarefas-safeguards.md (inventario normativo, nao validado contra codigo)
- **Task implementada:** TASK-01.1 - C1/I1 resolvidos; aguarda confirmação runtime final
- **Task concluida:** TASK-08.3 - Dockerização integral da stack
- **Validacao:** frontend smoke/build e backend em imagem Docker Java 25; Maven local bloqueado por JDK 21
- **Code review:** REPROVADO anteriormente — revalidar TASK-01.1 após TASK-08.3
- **Pendência:** repetir smoke Docker quando o daemon estiver disponível
- **Proximo comando:** `/code-review TASK-01.1` (revalidar criterio de execucao via Docker)