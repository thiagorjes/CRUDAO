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
- [x] TASK-02.1 — CRUD de Tarefa e movimentação entre etapas
- [x] TASK-02.2 — Tempo real (WebSocket/STOMP), broadcast multi-pod e observadores
- [x] TASK-04.1 — RBAC híbrido: papéis, permissões e integração Keycloak
- [x] TASK-03.1 — Cálculo de lead-time, impedimento e Dashboard assíncrono
- [x] TASK-05.0 — Frontend: Login via Keycloak (OIDC Authorization Code) — lacuna identificada em 2026-08-22
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

_Atualizado por: /code-review (agent QA) — 2026-08-22, a partir da TASK-05.0_
> Decisões: —

**Restrições:**
- **G-AUTH-01**: toda URL de redirect pós-login (`returnTo` ou equivalente) deve ser validada como path relativo interno antes de uso — nunca aceitar URL absoluta vinda de input do usuário/query string (open redirect). Ver `caminhoRelativoSeguro` em `frontend/src/lib/auth/return-to.ts`.
- **G-AUTH-02**: rotas `route.ts` de fluxo OIDC (login/callback/logout) e o proxy autenticado devem ter teste automatizado cobrindo os ramos de erro (state inválido, refresh falho, cookie malformado) — não só as libs puras que elas chamam.
- **G-AUTH-03**: `COOKIE_SECURE=true` é obrigatório em qualquer ambiente com TLS. Guardrail de log implementado (`session.ts` avisa se `NODE_ENV=production` sem `COOKIE_SECURE=true`) — reforçar com checklist de release quando um TLS termination for introduzido.
- **G-AUTH-04**: renovação de `refresh_token` deve ser resiliente a falha (nunca propagar exceção crua como 500 — sempre 401 explícito) e deduplicada por chave (refresh_token) para evitar corrida em rotação de refresh token. Ver `garantirSessaoValida` em `frontend/src/lib/auth/renovacao.ts`.

**O que NÃO fazer:**
- Não expor `access_token`/`refresh_token`/`id_token` ao JS do browser — toda chamada autenticada à API passa pelo proxy server-side (`/api/proxy/[...path]`), nunca por fetch direto do client com o token.
- Não usar `NODE_ENV === 'production'` como proxy para "servir por HTTPS" — são coisas independentes neste deploy (Docker on-premise sem TLS nesta fase). Usar env var dedicada (`COOKIE_SECURE`).
- Não confiar em validação feita apenas na escrita de um valor vindo de input do usuário (ex.: `returnTo`) — revalidar também no ponto de uso (defesa em profundidade), como feito em `callback/route.ts`.
