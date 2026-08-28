# Estado Operacional - CRUDAO
_Atualizado em: 2026-08-28_

> Estado atual do workspace e das features em andamento.
> Para principios estaveis e ADRs, veja [memory/constitution.md](constitution.md).

---

## Toolset

**Versao:** 2026-08-28

**Pipeline SSPDD:** /guidelines -> /discovery -> /prd -> [/clarify] -> [/checklist] -> [/designer] -> /techspec -> /tasks -> [/analyze] -> /implement ou /tdd -> /code-review -> /tests -> [/spdd-sync]

---

## Sistemas

| Sistema | Caminho | Cenario | Guidelines | Observacoes |
|---|---|---|---|---|
| CRUDAO | `systems/CRUDAO/` | Novo (greenfield) | ok | Backend Spring Boot + frontend Next.js |

## Features Ativas

| Feature | Sistemas afetados | PRD | TechSpec | Tasks | Status |
|---|---|---|---|---|---|
| kanban-tarefas | CRUDAO | 1.0 | 1.1 | 1.1 | Em implementação |

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
| docs/checklists/kanban-tarefas-TASK-03.2-review.md | 1.0 | ok |
| docs/checklists/kanban-tarefas-TASK-03.3-review.md | 1.0 | ok |
| docs/spdd/kanban-tarefas-canvas.md | — | draft - Safeguards atualizados via /code-review |

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
| 2026-08-28 | /tdd TASK-03.2 concluído: CRUD Workflow/Etapa/Transicao implementado com suíte de testes 100% verde no Docker |
| 2026-08-28 | /code-review TASK-03.2 concluído: APROVADO sem ressalvas |
| 2026-08-28 | /implement TASK-03.3 concluído: CRUD Raia com suíte de testes 100% verde no Docker |
| 2026-08-28 | /code-review TASK-03.3 concluído: APROVADO sem ressalvas (0 críticos, 0 importantes, 1 sugestão) |

## kanban-tarefas

- **Tasks Concluídas:**
  - `TASK-01.1` — Setup de projeto + Docker Compose + Keycloak dev
  - `TASK-01.2` — Migrations V1-V2 (Usuario, Projeto, Papel, Permissao)
  - `TASK-02.1` — OIDC + JIT Provisioning + /api/me + Logout
  - `TASK-02.2` — Motor RBAC + PermissaoGuard
  - `TASK-03.1` — CRUD de Projeto (incl. finalizar/reabrir)
  - `TASK-03.2` — CRUD Workflow/Etapa/Transicao (TDD + Review Aprovado)
  - `TASK-03.3` — CRUD Raia (TDD + Review Aprovado)
  - `TASK-08.3` — Dockerização de backend e frontend
- **Última Etapa:** `/code-review TASK-03.3` — APROVADO sem ressalvas em 2026-08-28
- **Testes:** 35 testes executados via Maven no Docker, 0 falhas e 0 erros
- **Próximo passo recomendado:** `/implement TASK-04.1` ou `/tdd TASK-04.1` (Migrations de Tarefas e Criação de Card)