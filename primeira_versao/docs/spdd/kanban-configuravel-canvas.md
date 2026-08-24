# REASONS Canvas — Kanban Configurável
_Status: READY | Idioma: pt_BR | Iniciado em: 2026-08-22 | Todas as 7 dimensões preenchidas em: 2026-08-23_

> **Como usar:** cada skill do pipeline preenche suas dimensões ao concluir.
> Canvas transita de DRAFT → READY quando todas as 7 dimensões estiverem preenchidas.
> READY = canvas executável por outro agente de implementação.

---

## R — Requirements

_Atualizado por: /clarify v1.2 — 2026-08-23_
> Decisões: BDR-001

**RBAC por projeto (clarificação v1.2):** papel `admin` é global; `project_admin`, `product_owner`, `dev`, `gestor` e `user` (legado) são atribuídos por par (usuário, projeto), acumulando permissões quando há mais de um papel no mesmo projeto. `project_admin` administra usuários do seu projeto e toggles de comportamento (RF-016), mas não cria papéis/permissões (exclusivo do admin global). Detalhes: RF-013, RF-015 a RF-017, RN-008 a RN-016, ver [BDR-001](../decisions/BDR-001-rbac-por-projeto.md).

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

_Atualizado por: /techspec v1.1 — 2026-08-23_
> Decisões: DDR-001, DDR-002, DDR-003, BDR-001

**Novas entidades de RBAC por projeto (v1.1):** `UsuarioProjetoPapel` (usuário↔projeto↔papel, chave composta, múltiplos papéis acumuláveis por par), `ConfiguracaoProjeto` (3 toggles booleanos por projeto — `devPodeExcluirTarefa`, `devPodeEditarTarefaIniciada`, `gestorVeBoard`), `AuditoriaTarefa` (log genérico: campo/valor anterior/valor novo/autor/data). `Usuario` ganha flag `admin` (substitui `papel_id` único); `Projeto` ganha `data_finalizacao`. Detalhes em [data-model.md](../techspec/kanban-configuravel/data-model.md).

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

_Atualizado por: /techspec v1.1 — 2026-08-23_
> Decisões: ADR-001, ADR-002, ADR-004, ADR-005, ADR-006

**Estratégia de solução:**
REST + WebSocket/STOMP (event-driven para tempo real). Broadcast de eventos entre pods via PostgreSQL LISTEN/NOTIFY, sem broker dedicado nesta fase. Dashboard calculado de forma assíncrona (`@Async` + entrega via STOMP) para evitar timeout em agregações sobre período longo. **(v1.1)** Autorização escopada por projeto validada explicitamente no Service (`AutorizacaoProjetoService`), não mais só via AOP genérico (`@ExigePermissao` fica restrito a ações globais) — ADR-006.

**Trade-offs aceitos:**
- Sem cache/broker (Redis) nesta fase — simplicidade em troca de risco de escala a validar (Q-001 na techspec)
- LISTEN/NOTIFY limitado a 8KB de payload — eventos carregam apenas IDs, exigindo fetch adicional
- **(v1.1)** Checagem de permissão por projeto não é 100% garantida "por construção" (não é mais um único aspecto AOP cobrindo tudo) — mitigado por teste de integração dedicado a 403 em cada endpoint de escrita e checklist de code review (ADR-006)

---

## S — Structure

_Atualizado por: /techspec v1.1 — 2026-08-23_
> Decisões: ADR-001, ADR-003, ADR-006, BDR-001

**Arquitetura:**
Backend Spring Boot (Controller/Service/Repository + Mapper via MapStruct) com API REST e endpoint STOMP; frontend Next.js. PostgreSQL como única fonte de estado, acessada por N pods do backend. Autenticação delegada ao Keycloak (OIDC); autorização (papéis/permissões) modelada na aplicação. **(v1.1)** RBAC passa a ser escopado por projeto: `Usuario.admin` (flag global) + `UsuarioProjetoPapel` (papéis por par usuário-projeto, acumuláveis) substituem o vínculo único `Usuario→Papel` da TASK-04.1; `AutorizacaoProjetoService` centraliza a checagem, chamada explicitamente pelos Services de domínio (não mais só por AOP genérico).

**Dependências externas:**
- Keycloak (OIDC) — contrato validado: [CRUDAO-keycloak-contract.md](../contracts/CRUDAO-keycloak-contract.md)

---

## O — Operations

_Atualizado por: /tasks v1.1 — 2026-08-23_
> Decisões: BDR-001

**Tasks ordenadas por dependência (sequencial, sem paralelismo):**
- [x] TASK-00.1 — Provisionar Keycloak via Docker
- [x] TASK-00.2 — Setup do projeto base (backend, frontend, PostgreSQL)
- [x] TASK-01.1 — Modelo de domínio: Projeto, Workflow, Etapa e Transição
- [x] TASK-01.2 — Raias (swimlanes) por projeto e default globais
- [x] TASK-02.1 — CRUD de Tarefa e movimentação entre etapas
- [x] TASK-02.2 — Tempo real (WebSocket/STOMP), broadcast multi-pod e observadores
- [x] TASK-04.1 — RBAC híbrido: papéis, permissões e integração Keycloak (retrabalhada por TASK-04.2)
- [x] TASK-03.1 — Cálculo de lead-time, impedimento e Dashboard assíncrono
- [x] TASK-05.0 — Frontend: Login via Keycloak (OIDC Authorization Code) — lacuna identificada em 2026-08-22
- [x] TASK-05.1 — Frontend: Board principal
- [x] TASK-05.2 — Frontend: Dashboard de gestão
- [x] TASK-04.2 — RBAC por projeto: retrabalho do modelo e enforcement _(nova, PRD v1.3, BDR-001)_
- [x] TASK-01.3 — Configuração de projeto (toggles) e finalização _(nova)_
- [x] TASK-02.3 — Regras avançadas de tarefa: edição travada, atribuição, finalização e auditoria _(nova)_
- [x] TASK-05.3 — Frontend: Painel de Administração _(retrabalhada — interrompida na implementação original, gap de RBAC por projeto)_
- [x] TASK-05.4 — Frontend: ajustes de tarefa para RBAC por projeto _(nova)_
- [x] TASK-06.1 — Testes E2E dos fluxos principais e revisão de cobertura _(revisado por DEV-002 — /spdd-sync 2026-08-24)_

---

## N — Norms

_Atualizado por: /techspec v1.1 — 2026-08-23_
> Decisões: —

**Padrões relevantes para esta feature (extraídos de systems/CRUDAO/guidelines):**
- Java 25 + Spring Boot LTS, Lombok, MapStruct, Bean Validation (stack.md)
- PostgreSQL como fonte única de verdade, sem cache/broker nesta fase (stack.md, architecture.md)
- TDD 80% / BDD 100% para cenários Gherkin do PRD, obrigatório sempre que aplicável (testing.md)
- RBAC híbrido Keycloak + permissões internas; papel admin protegido (security.md)
- Logs em arquivo local com rotação 5MB, retendo os 10 mais recentes (observability.md)
- GitFlow simplificado, sem CI/CD nesta fase — commit/push liberado após testes e lint locais (git-workflow.md)
- **(v1.1)** Autorização escopada a projeto sempre resolvida a partir da entidade carregada no backend, nunca de dado do payload do cliente (RNF-003 reforçada, ADR-006) — mesmo padrão de "revalidar no ponto de uso" já usado no frontend (`return-to.ts`, TASK-05.0)

---

## S — Safeguards

_Atualizado por: /code-review — 2026-08-24, a partir da TASK-06.1_
> Decisões: —

**Restrições:**
- **G-AUTH-01**: toda URL de redirect pós-login (`returnTo` ou equivalente) deve ser validada como path relativo interno antes de uso — nunca aceitar URL absoluta vinda de input do usuário/query string (open redirect). Ver `caminhoRelativoSeguro` em `frontend/src/lib/auth/return-to.ts`.
- **G-AUTH-02**: rotas `route.ts` de fluxo OIDC (login/callback/logout) e o proxy autenticado devem ter teste automatizado cobrindo os ramos de erro (state inválido, refresh falho, cookie malformado) — não só as libs puras que elas chamam.
- **G-AUTH-03**: `COOKIE_SECURE=true` é obrigatório em qualquer ambiente com TLS. Guardrail de log implementado (`session.ts` avisa se `NODE_ENV=production` sem `COOKIE_SECURE=true`) — reforçar com checklist de release quando um TLS termination for introduzido.
- **G-AUTH-04**: renovação de `refresh_token` deve ser resiliente a falha (nunca propagar exceção crua como 500 — sempre 401 explícito) e deduplicada por chave (refresh_token) para evitar corrida em rotação de refresh token. Ver `garantirSessaoValida` em `frontend/src/lib/auth/renovacao.ts`.
- **G-RT-01** (TASK-05.1): conexões WebSocket/STOMP devem exigir credencial válida no frame CONNECT (header `Authorization: Bearer`, validado via o mesmo `JwtDecoder` da API REST) — o handshake HTTP de upgrade pode ser `permitAll()` (necessário para o upgrade em si), mas isso não dispensa autenticação no nível do protocolo STOMP. Ver `StompAuthChannelInterceptor` (backend) e `conectarBoard`/`GET /api/ws-token` (frontend). Autorização por projeto (restringir subscription a membros do projeto) ainda não é aplicada — **atualização v1.1:** RBAC deixa de ser só global (BDR-001, ADR-006) — quando `UsuarioProjetoPapel` for implementado (retrabalho TASK-04.1), este guardrail deve ser estendido para também restringir a subscription STOMP a membros do projeto, reaproveitando `AutorizacaoProjetoService`.
- **G-RBAC-05**: DTOs de leitura pública (ex.: `UsuarioDTO`) não devem expor PII (e-mail, etc.) além do estritamente necessário ao consumidor — minimização de dados. `UsuarioDTO` expõe só `id`/`nome`.
- **G-FE-01**: todo fluxo de drag-and-drop deve implementar `onDragEnd` para garantir limpeza de estado de UI (destaque de coluna, item arrastando) mesmo em cancelamento ou drop fora de área válida.
- **G-RT-02** (TASK-05.2): todo fluxo que dispara um job assíncrono e depende de STOMP para entregar o resultado deve fazer uma consulta de "catch-up" (`GET` do recurso do job) assim que a subscription é confirmada (`onConnect`) — não apenas quando a conexão falha. Jobs rápidos podem publicar via `pg_notify` antes do handshake STOMP terminar; nesse caso a conexão não "falha", então um fallback disparado só por `aoFalhar` nunca é acionado e a UI trava em loading. Ver `aoConectar` em `conectarDashboard` (`frontend/src/lib/board/realtime.ts`) e `buscarResultadoAgora` em `DashboardApp.tsx`.
- **G-RBAC-06** (v1.2, TechSpec — comitê de análise, achado convergente security+architect): toda checagem de autorização escopada a projeto (`AutorizacaoProjetoService.exigirPermissao`) deve ter cobertura garantida por um teste estrutural de CI (não só teste de integração pontual por endpoint) — verificando que todo método público de `@Service` de domínio que grava entidade com `projetoId` contém a chamada. Ver ADR-006.
- **G-RBAC-07** (v1.2, TechSpec — comitê de análise, achado security): `papel:gerenciar` nunca é atribuível via `UsuarioProjetoPapel` — é checada exclusivamente contra `Usuario.admin`, nunca contra papel de projeto. `PUT /api/projetos/{id}/membros/{usuarioId}` deve rejeitar qualquer papel cuja permissão inclua `papel:gerenciar` (além do papel `admin` em si). RN-006 do PRD superseded em parte por este guardrail (PRD v1.3).
- **G-RBAC-08** (v1.2, TechSpec — comitê de análise, achado architect): a checagem de "projeto finalizado" (RN-015) deve viver dentro de `AutorizacaoProjetoService.exigirPermissao` (ponto único), não replicada como validação independente em cada Service — evita a mesma classe de esquecimento silencioso do G-RBAC-06.
- **G-FE-02** (TASK-05.3, code review agent QA): gating de UI baseado em permissões (`GET /api/usuarios/me`) deve preferir checagem direta (`permissoes.includes(chave)` / `admin || permissoesProjeto.has(chave)`) a listas sintéticas hardcoded por papel (ex.: `if (admin) return new Set([...chaves fixas...])`) — reduz risco de uma nova tela esquecer de estender a lista sintética ao introduzir uma nova chave de permissão. Ver `AdminApp.tsx` (`permissoesProjeto`).
- **G-RBAC-09** (TASK-06.1, code review — achado exposto pela suíte E2E, débito técnico não bloqueante): todo ponto de auto-provisionamento "find-or-create" sob concorrência (ex.: `UsuarioContexto.provisionar`, primeiro login de um usuário) deve tratar a corrida entre a busca e a inserção — capturar `DataIntegrityViolationException` e recair para uma nova busca, ou usar `INSERT ... ON CONFLICT`. Mesma classe de problema já resolvida para refresh de token em G-AUTH-04; `RbacSeeder.buscarOuCriarPapel`/`buscarOuCriarPermissao` não sofrem disso por rodarem sequencialmente no startup, mas `UsuarioContexto.provisionar` roda sob requisição HTTP concorrente e ainda não tem a mesma proteção — reproduzido de forma determinística rodando a suíte E2E com workers paralelos contra um volume novo.
- **G-TEST-01** (TASK-06.1): specs E2E que localizam um elemento por texto renderizado devem preferir `getByTestId(...).filter({ hasText })` a encadear `.locator('..')` a partir do nó de texto — a segunda forma quebra com qualquer mudança de profundidade do DOM do componente, sem relação com o comportamento sendo testado. Ver `board.spec.ts`/`rbac.spec.ts` (`data-testid="card-tarefa"`).

**O que NÃO fazer:**
- Não expor `access_token`/`refresh_token`/`id_token` ao JS do browser para chamadas REST — toda chamada autenticada à API passa pelo proxy server-side (`/api/proxy/[...path]`), nunca por fetch direto do client com o token. **Exceção documentada:** `GET /api/ws-token` expõe o `access_token` ao client especificamente para o header CONNECT do STOMP (a lib roda no browser e conecta direto ao backend, sem passar pelo proxy) — o token não é persistido pelo client (sem localStorage/cookie), só usado em memória no momento do connect.
- Não usar `NODE_ENV === 'production'` como proxy para "servir por HTTPS" — são coisas independentes neste deploy (Docker on-premise sem TLS nesta fase). Usar env var dedicada (`COOKIE_SECURE`).
- Não confiar em validação feita apenas na escrita de um valor vindo de input do usuário (ex.: `returnTo`) — revalidar também no ponto de uso (defesa em profundidade), como feito em `callback/route.ts`.
- Não tratar `/ws/**` como "seguro por padrão" só porque `permitAll()` está no `SecurityConfig` — o `permitAll()` ali é sobre o handshake HTTP, não sobre autenticação STOMP (ver G-RT-01).
