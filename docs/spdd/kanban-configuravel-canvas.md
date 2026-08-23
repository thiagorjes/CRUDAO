# REASONS Canvas — Kanban Configurável
_Status: DRAFT | Idioma: pt_BR | Iniciado em: 2026-08-22_

> **Como usar:** cada skill do pipeline preenche suas dimensões ao concluir.
> Canvas transita de DRAFT → READY quando todas as 7 dimensões estiverem preenchidas.
> READY = canvas executável por outro agente de implementação.

---

## R — Requirements

_Atualizado por: /prd v1.0 — 2026-08-22_
> Decisões: —

**Objetivos de negócio:**
- Reduzir tempo de execução e de impedimento das atividades
- Eliminar comunicação dispersa sobre status/impedimentos
- Dar visibilidade de andamento e lead-time aos gestores

**RFs Must Have:** RF-001 a RF-013 (board configurável, workflows/transições, CRUD de tarefas/projetos/workflows/colunas/raias, impedimento, notificação a observadores, lead-time por etapa e de impedimento, dashboard, etapa final com reabertura, controle de acesso por papéis configuráveis)
**RFs Should Have:** RF-014 (login SSO via Keycloak)

**Escopo IN:**
- Board kanban com colunas e raias configuráveis por projeto
- Workflows com transições configuráveis entre etapas
- Atualização de status e sinalização de impedimentos pelos devs
- Notificação (interna) a observadores em transições
- Lead-time por etapa e lead-time de impedimento, visível na tarefa e no dashboard
- Controle de acesso por papéis configuráveis (além de admin/user padrão)
- Visualização por gestores de outros times sem necessidade de atualização

**Escopo OUT:**
- Integração com sistemas externos de notificação (email, Slack etc.)
- Controle de horas/timesheet do desenvolvedor
- Suporte a múltiplas organizações/clientes
- Dependência entre projetos (bloqueio cruzado)

---

## E — Entities

_Atualizado por: /designer v1.0 — 2026-08-22_
> Decisões: DDR-001, DDR-002, DDR-003

Ver diagrama completo em [data-model.md](../techspec/kanban-configuravel/data-model.md) e tokens/componentes em [design-brief.md](../design/kanban-configuravel-design-brief.md).

**Entidades de UX/UI (design brief):**
- Tokens: paleta (10 cores, primária `#0d6efd`, sucesso `#198754`), tipografia Roboto única, espaçamento base 8px, breakpoint único ≥1024px
- Componentes: Card de Tarefa (com indicador de impedimento), Coluna (com destaque drag válido), Raia, Modal de Confirmação, Toast/Snackbar, Skeleton Screen, Spinner, Gráfico de Barras + Tabela (dashboard), Seletor de Período, Painel de Administração, Menu do Card
- Padrões: drag-and-drop com destaque de colunas válidas + menu alternativo; feedback modal (erro) vs toast (sucesso); loading skeleton (assíncrono) vs spinner (síncrono)

**Entidades principais:**
- Projeto: contexto que agrupa workflow(s), raias e tarefas
- Workflow: define as etapas e transições permitidas de um projeto
- Etapa (coluna): ordenada, com flag `e_final`; medida de lead-time
- Transição: liga etapa origem→destino, tipo `NORMAL` ou `REABERTURA`
- Raia (Swimlane): específica de projeto ou default global
- Tarefa: unidade de trabalho, com tipo, responsável, etapa/raia atual
- RegistroEtapa: histórico de permanência da tarefa em cada etapa (lead-time)
- Impedimento: período de bloqueio vinculado a um RegistroEtapa
- Observador: usuário vinculado a uma tarefa para notificação
- Usuário / Papel / Permissão: RBAC configurável (papel `admin` protegido)

---

## A — Approach

_Atualizado por: /techspec v1.0 — 2026-08-22_
> Decisões: ADR-001, ADR-002, ADR-004, ADR-005

**Estratégia de solução:**
REST + WebSocket/STOMP (event-driven para tempo real). Broadcast de eventos entre pods via PostgreSQL LISTEN/NOTIFY, sem broker dedicado nesta fase. Dashboard calculado de forma assíncrona (`@Async` + entrega via STOMP) para evitar timeout em agregações sobre período longo.

**Trade-offs aceitos:**
- Sem cache/broker (Redis) nesta fase — simplicidade em troca de risco de escala a validar (Q-001 na techspec)
- LISTEN/NOTIFY limitado a 8KB de payload — eventos carregam apenas IDs, exigindo fetch adicional

---

## S — Structure

_Atualizado por: /techspec v1.0 — 2026-08-22_
> Decisões: ADR-001, ADR-003

**Arquitetura:**
Backend Spring Boot (Controller/Service/Repository + Mapper via MapStruct) com API REST e endpoint STOMP; frontend Next.js. PostgreSQL como única fonte de estado, acessada por N pods do backend. Autenticação delegada ao Keycloak (OIDC); autorização (papéis/permissões) modelada na aplicação.

**Dependências externas:**
- Keycloak (OIDC) — contrato validado: [CRUDAO-keycloak-contract.md](../contracts/CRUDAO-keycloak-contract.md)

---

## O — Operations

_Atualizado por: /tasks v1.0 — 2026-08-22_
> Decisões: —

**Tasks ordenadas por dependência (sequencial, sem paralelismo):**
- [ ] TASK-00.1 — Provisionar Keycloak via Docker
- [ ] TASK-00.2 — Setup do projeto base (backend, frontend, PostgreSQL)
- [ ] TASK-01.1 — Modelo de domínio: Projeto, Workflow, Etapa e Transição
- [x] TASK-01.2 — Raias (swimlanes) por projeto e default globais
- [ ] TASK-02.1 — CRUD de Tarefa e movimentação entre etapas
- [ ] TASK-02.2 — Tempo real (WebSocket/STOMP), broadcast multi-pod e observadores
- [ ] TASK-04.1 — RBAC híbrido: papéis, permissões e integração Keycloak
- [ ] TASK-03.1 — Cálculo de lead-time, impedimento e Dashboard assíncrono
- [ ] TASK-05.1 — Frontend: Board principal
- [ ] TASK-05.2 — Frontend: Dashboard de gestão
- [ ] TASK-05.3 — Frontend: Painel de Administração
- [ ] TASK-06.1 — Testes E2E dos fluxos principais e revisão de cobertura

---

## N — Norms

_Atualizado por: /techspec v1.0 — 2026-08-22_
> Decisões: —

**Padrões relevantes para esta feature (extraídos de systems/CRUDAO/guidelines):**
- Java 25 + Spring Boot LTS, Lombok, MapStruct, Bean Validation (stack.md)
- PostgreSQL como fonte única de verdade, sem cache/broker nesta fase (stack.md, architecture.md)
- TDD 80% / BDD 100% para cenários Gherkin do PRD, obrigatório sempre que aplicável (testing.md)
- RBAC híbrido Keycloak + permissões internas; papel admin protegido (security.md)
- Logs em arquivo local com rotação 5MB, retendo os 10 mais recentes (observability.md)
- GitFlow simplificado, sem CI/CD nesta fase — commit/push liberado após testes e lint locais (git-workflow.md)

---

## S — Safeguards

_Atualizado por: /code-review v1.0 — {{DATE}}_
> Decisões: —

**Restrições:**
- {{RESTRICAO_1}}

**O que NÃO fazer:**
- {{NAO_FAZER_1}}
