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
| kanban-configuravel | CRUDAO | 1.1 | 1.0 | 1.0 | Em implementação — TASK-00.1, TASK-00.2, TASK-01.1 concluídas, próxima: TASK-01.2 |

---

## Implementação — kanban-configuravel

- **Task implementada:** TASK-00.1 — Provisionar Keycloak via Docker — 2026-08-22
- **Arquivos:** `docker-compose.yml`, `infra/keycloak/crudao-realm.json`, `docs/contracts/CRUDAO-keycloak-contract.md` (substitui o mock)
- **Testes:** validação manual (não aplicável a TDD — task de infraestrutura); login via password grant e claims conferidas
- **Nota técnica:** `realm_access.roles` está no `access_token`, não no `id_token` — considerar na TASK-04.1

- **Task implementada:** TASK-00.2 — Setup do projeto base (backend, frontend, PostgreSQL) — 2026-08-22
- **Arquivos:** `backend/` (Spring Boot 3.5.16, Java 25), `frontend/` (Next.js 16.3.2), `docker-compose.yml` (+ postgres, backend, frontend)
- **Testes:** backend (JUnit5, Spotless) e frontend (Vitest, ESLint) passando; stack completa validada via `docker compose up -d --build`
- **Nota técnica:** versões de Spring Boot/Lombok/Spotless fixadas por compatibilidade com Java 25 — detalhes em `systems/CRUDAO/guidelines/stack.md` e na própria task
- **Task implementada:** TASK-01.1 — Modelo de domínio: Projeto, Workflow, Etapa e Transição — 2026-08-22
- **Arquivos:** `backend/src/main/java/com/crudao/kanban/domain/{projeto,workflow}/*`, `backend/src/main/java/com/crudao/kanban/common/*`, testes em `backend/src/test/java/.../workflow/*`
- **Testes:** 6 testes TDD (`TransicaoEngineTest`) + `WorkflowFluxoIT` (Testcontainers) — validados via `mvn test` (unitários) e fluxo REST real via `docker compose`
- **Achado técnico:** campo `eFinal` renomeado para `etapaFinal` — colisão com convenção JavaBeans quebrava serialização silenciosamente (ver `coding-standards.md`)
- **Nota RN-005:** verificação de tarefas ativas é uma porta (`VerificadorDeTarefasAtivas`) ainda sem implementação real — TASK-02.1 deve substituí-la

- **Task implementada:** TASK-01.2 — Raias (swimlanes) por projeto e default globais — 2026-08-22
- **Arquivos:** `backend/src/main/java/com/crudao/kanban/domain/raia/*` (Raia, RaiaResolver, RaiaRepository, RaiaService, RaiaController, RaiaMapper, RaiaDTO/Request), `common/VerificadorDeTarefasAtivas.java` (+ `existemTarefasNaRaia`)
- **Testes:** TDD (`RaiaResolverTest`, 3 casos) via `mvn test`; fluxo REST validado via `docker compose` (raia própria vs. fallback para default global, exclusão)
- **Próxima task:** TASK-02.1 — CRUD de Tarefa e movimentação entre etapas

---

## Artifact Registry

| Artefato | v | Status |
|---|---|---|
| docs/discovery/kanban-configuravel-discovery.md | 1.0 | ok |
| docs/prd/kanban-configuravel-prd.md | 1.1 | ok |
| docs/techspec/kanban-configuravel-techspec.md | 1.0 | ok |
| docs/techspec/kanban-configuravel/data-model.md | 1.0 | ok |
| docs/contracts/CRUDAO-keycloak-contract.md | 1.0 | ok (validado na TASK-00.1) |
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
