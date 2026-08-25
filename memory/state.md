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
| docs/tasks/kanban-tarefas-tasks.md | 1.0 | ok |
| docs/spdd/kanban-tarefas-canvas.md | — | draft (7/7 dimensões preenchidas — falta S/Safeguards de /code-review para READY) |

---

## Evolução do SDD

| Data | Mudança |
|---|---|
| 2026-08-24 | Workspace inicializado via init.py |
| 2026-08-25 | /prd kanban-tarefas concluído (v1.0) |
| 2026-08-25 | /designer kanban-tarefas concluído (v1.0) |
| 2026-08-25 | /techspec kanban-tarefas concluído (v1.0) — ADR-004/005/006 criados |
| 2026-08-25 | /tasks kanban-tarefas concluído (v1.0) — 24 tasks em 8 epics |

---

## kanban-tarefas

- **Etapa concluída:** /implement TASK-01.1 — 2026-08-25
- **Task implementada:** TASK-01.1 — Setup projeto backend/frontend + docker-compose + Keycloak dev — 2026-08-25
- **Arquivos:** systems/CRUDAO/backend/** (pom.xml, KanbanApplication, HealthController, application.yml/application-dev.yml, Dockerfile), systems/CRUDAO/frontend/** (Next.js skeleton, Dockerfile), systems/CRUDAO/docker-compose.yml, systems/CRUDAO/keycloak/realm-export.json, systems/CRUDAO/README.md
- **Testes:** não aplicável (task de configuração/skeleton, TDD não se aplica) — validação sintática de pom.xml/docker-compose.yml/realm-export.json ok; execução real (mvnw/npm/docker up) não realizada neste ambiente sandbox (sem acesso a Maven Central/npm registry/Docker daemon) — validar localmente antes de considerar 100% fechada
- **Code review:** inline via agent general-purpose (papel QA) — 1 finding 🔴 corrigido (colisão de porta 8080 backend×Keycloak → backend movido para 8081), demais 🟡 são notas de dev-only já documentadas
- **Próxima task:** TASK-01.2 — Migrations V1-V2: Usuario/Projeto/Papel/Permissao
- **Artefato:** docs/tasks/kanban-tarefas-tasks.md + docs/tasks/kanban-tarefas/TASK-*.md (24 arquivos)
- **Total de tasks:** 25 tasks em 8 epics (Infra, Auth/RBAC, Projetos/Workflows/Raias, Tarefas core, Tempo real/Notificações, Dashboard, Frontend, Hardening) — revisado pelo Comitê de Análise (Architect+QA): TASK-02.2 desmembrada em 02.2+02.3; correções de RN-005, RN-012, TDD obrigatório, RNF-005 e grafo de dependências aplicadas
- **Granularidade:** maior (decisão do usuário) — tasks G (1-2 dias) predominantes, poucas M/P
- **Canvas:** DRAFT (dimensão O preenchida; falta S/Safeguards de /code-review para READY)
- **Nota de escopo confirmada com usuário:** setup do Keycloak (realm/client) para ambiente local de dev está em TASK-01.1 — Keycloak (IdP) em si é premissa externa já disponível (ADR-006); não há menção de time externo responsável na TechSpec.
- **Sistemas afetados:** CRUDAO
- **ADRs/artefatos herdados de /techspec:** ADR-004, ADR-005, ADR-006; comitê de análise assíncrono já aplicado (13 achados)
- **Decisão em aberto:** densidade do card no board (compacto vs. expandido, TL-03/TL-03b) — não bloqueia implementação
- **Nota:** ADR-001/002/003 referenciados nas guidelines mas ausentes em docs/decisions/ (pré-existente) — recriar antes de auditoria formal
- **Próximo comando:** /implement TASK-01.1 (ou /tdd para tasks marcadas TDD obrigatório, ex. TASK-04.2)
