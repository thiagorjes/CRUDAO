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
| kanban-configuravel | CRUDAO | 1.1 | 1.0 | 1.0 | Em implementação — EPIC-00/01/02 concluídos (até TASK-02.2), próxima: TASK-04.1 ou TASK-03.1 |

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
- **Task implementada:** TASK-02.1 — CRUD de Tarefa e movimentação entre etapas — 2026-08-22
- **Arquivos:** `backend/src/main/java/com/crudao/kanban/domain/tarefa/*` (Tarefa, TipoTarefa, Repository, Service, Controller, Mapper, DTOs/Requests, VerificadorDeTarefasAtivasImpl); `common/VerificadorDeTarefasAtivas.java` (virou interface); `domain/projeto/ProjetoController.java` (+ endpoint `PUT /{id}/workflow-ativo`, expondo método de serviço já existente mas não roteado)
- **Testes:** TDD (`TarefaServiceTest`, 8 casos — mover permitido/proibido/reabertura, impedimento independente de etapa, mover entre projetos) via `mvn test`; fluxo REST completo validado via `docker compose` (2 projetos, workflows, transições, RN-005, mover-projeto)
- **Code review:** agent QA — aprovado com ressalvas; 3 findings 🟡 corrigidos (testes de `moverParaProjeto` e REABERTURA adicionados, comentário sobre `motivo` não persistido)
- **Nota técnica:** RegistroEtapa/Impedimento (histórico de lead-time) ficam para TASK-03.1; RBAC de `mover-projeto` fica para TASK-04.1 (TODO no código); nenhum endpoint do projeto tem controle de acesso ainda (`permitAll()` global, decisão da TASK-00.2)

- **Task implementada:** TASK-02.2 — Tempo real (WebSocket/STOMP), broadcast multi-pod e observadores — 2026-08-22
- **Arquivos:** `backend/src/main/java/com/crudao/kanban/config/WebSocketConfig.java`; `backend/src/main/java/com/crudao/kanban/realtime/*` (TipoEventoBoard, NotificacaoMinima, EventoBoardDTO, EventoBoardPublisher, PostgresNotificationListener); `domain/tarefa/{Observador,ObservadorRepository}.java`; `domain/tarefa/{TarefaService,TarefaController}.java` (hooks de publicação + endpoints de observadores); `pom.xml` (driver `postgresql` movido de `runtime` para `compile` — listener usa `PGConnection`/`PGNotification` diretamente)
- **Testes:** `RealtimeBoardIT` (Testcontainers + 2 clientes STOMP reais) validando entrega em até 2s; suíte unitária completa (18 testes) verde; fluxo REST completo (observadores + movimentação) validado via `docker compose`
- **Nota técnica:** listener mantém conexão JDBC dedicada via `DriverManager` (fora do pool Hikari, requisito do `LISTEN`) com reconexão automática; publicação do evento ocorre em `afterCommit` da transação para garantir que o pod receptor já encontre a linha persistida ao consultar o banco
- **Code review:** agent QA — 1 finding 🔴 corrigido (excluir tarefa com observador violava FK — `TarefaService.excluir` agora remove observadores antes) e 3 findings 🟡 corrigidos (endpoint STOMP duplicado em `WebSocketConfig`, falha silenciosa de `pg_notify` agora logada em vez de `@SneakyThrows`, `TransactionTemplate` do listener marcado `readOnly`); adicionado teste de observadores no `RealtimeBoardIT`
- **Task implementada:** TASK-04.1 — RBAC híbrido: papéis, permissões e integração Keycloak — 2026-08-22
- **Arquivos:** `backend/src/main/java/com/crudao/kanban/domain/rbac/*` (Usuario, Papel, Permissao, repositories, PapelService, PapelController, PapelDTO/Request, PapelMapper, RbacSeeder); `backend/src/main/java/com/crudao/kanban/security/*` (ExigePermissao, PermissaoAspect, UsuarioContexto, KeycloakJwtAuthenticationConverter, LocalUserDetailsService, UsuarioLocalDetails); `common/AcessoNegadoException.java` (+ `ApiExceptionHandler`); `config/SecurityConfig.java` (reescrito — exige autenticação real, login local via HTTP Basic como fallback); `pom.xml` (+ `spring-boot-starter-aop`); `@ExigePermissao` aplicado nos endpoints de escrita de Projeto/Raia/Workflow/Etapa/Transicao/Tarefa
- **Testes:** TDD (`PapelServiceTest` 6 casos incl. RN-006, `UsuarioContextoTest` 4 casos, `PermissaoAspectTest` 2 casos) via `mvn test` (30/30 verdes); fluxo REST completo validado via `docker compose up` — login real via Keycloak (admin.teste/user.teste), auto-provisionamento de Usuário no primeiro acesso, 403 para `user` sem permissão, 409 ao tentar excluir o papel `admin` (RN-006)
- **Nota técnica:** granularidade de permissão (Q-003) fechada em 6 chaves (`projeto:gerenciar`, `workflow:gerenciar`, `tarefa:gerenciar`, `impedimento:marcar`, `papel:gerenciar`, `dashboard:visualizar`); fallback de autenticação local implementado via HTTP Basic + `UsuarioRepository.senhaHash` (BCrypt), coexistindo com o resource server JWT — não há chaveamento automático de "Keycloak indisponível", ambos os caminhos ficam sempre disponíveis; `Observador` continua referenciando `usuarioId` solto (fora do escopo desta task)
- **Nota de ambiente:** `mvn`/testes rodados via container Docker (`maven:3.9-eclipse-temurin-25`) — o `~/.m2/settings.xml` deste ambiente aponta para um mirror interno inacessível (`nexus3-cicd-tools.cloud.sfb`); os testes `*IT` (Testcontainers) não foram re-executados nesta sessão por limitação de rede aninhada Docker-in-Docker do ambiente, não relacionada ao código desta task — validação de integração feita via `docker compose up` real
- **Próxima task:** TASK-05.1/05.2/05.3 (frontend) ou TASK-03.1 — Lead-time/Dashboard, a definir com o usuário

## Reorganização — código movido para systems/CRUDAO/

- **Mudança:** `backend/` e `frontend/` movidos da raiz do workspace para `systems/CRUDAO/backend/` e `systems/CRUDAO/frontend/` — 2026-08-22
- **Motivo:** alinhar com a convenção do framework SSPDD (`systems/[sistema]/` é o repositório do sistema, código incluído — não só guidelines). As skills `/tasks`, `/implement`, `/tdd`, `/code-review` foram atualizadas no framework para sempre operar dentro de `systems/[sistema]/`; `.agents/` deste workspace foi sincronizado via `scripts/update.py`
- **Ajustes:** `docker-compose.yml` (build context de `backend`/`frontend` → `systems/CRUDAO/backend`/`systems/CRUDAO/frontend`); `.gitignore` (paths de `backend/target`, `frontend/node_modules` etc. atualizados)
- **Não movido:** `infra/keycloak/` e `docker-compose.yml` permanecem na raiz — são orquestração do workspace, não código do sistema
- **Não alterado:** referências históricas a `backend/`/`frontend/` em `docs/tasks/kanban-configuravel*.md` (documentam o estado no momento da implementação; tasks futuras usam paths de pacote sem prefixo, ex: `domain/raia`, já compatíveis com a nova convenção)
- **Pendente de verificação:** rodar `docker compose up -d --build` para confirmar que os novos contextos de build funcionam antes do próximo `/implement`

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
