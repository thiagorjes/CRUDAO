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
| kanban-configuravel | CRUDAO | 1.3 | 1.3 | 1.1 | Implementação completa (16/16 tasks, 00.1 a 06.1) — E2E cobrindo fluxos críticos e RBAC por projeto; code review ainda não executado |

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
- **Task implementada:** TASK-03.1 — Cálculo de lead-time, impedimento e Dashboard assíncrono — 2026-08-22
- **Arquivos:** `backend/src/main/java/com/crudao/kanban/domain/leadtime/*` (RegistroEtapa, Impedimento, repositories, RegistroEtapaService, RegistroEtapaDTO, LeadTimeController); `backend/src/main/java/com/crudao/kanban/dashboard/*` (DashboardJob/Repository, DashboardService, DashboardController, DashboardCalcularRequest, DashboardResultadoDTO, StatusJobDashboard, DashboardEventoPublisher, DashboardNotificationListener); `config/AsyncConfig.java` (executor dedicado `dashboardExecutor`); `domain/tarefa/TarefaService.java` (hooks de abrir/fechar RegistroEtapa e Impedimento em criar/mover/moverParaProjeto/marcarImpedimento/desmarcarImpedimento)
- **Testes:** TDD (`RegistroEtapaServiceTest`, 7 casos incl. múltiplos períodos de impedimento intercalados) via `mvn test` (37/37 verdes); fluxo REST completo validado via `docker compose up` (criação de tarefa abre registro, impedimento acumula tempo, mover fecha/abre registro, dashboard 202 + polling retorna médias corretas por etapa)
- **Nota técnica:** `DashboardJob` persistido no PostgreSQL (não em memória) para que o polling funcione independente de qual pod atendeu a requisição, consistente com ADR-002 (fonte única de estado); resultado do dashboard entregue via canal LISTEN/NOTIFY dedicado (`dashboard_eventos`), reaproveitando o padrão multi-pod do ADR-004 — carrega o payload agregado completo (não só IDs), pois o objetivo é evitar nova consulta ao banco pelo pod receptor
- **Nota de ambiente:** validação REST via `docker compose` exigiu obter o token OIDC de dentro da rede Docker (`http://keycloak:8080`) para que o claim `iss` do JWT bata com o `KEYCLOAK_ISSUER_URI` do backend — token obtido via `localhost:8081` falha com 401 por mismatch de issuer; tabela `usuario` tinha registro remanescente de sessão anterior colidindo por email (keycloak_sub antigo), removida com autorização do usuário para reproduzir o fluxo limpo
- **Task implementada:** TASK-05.0 — Frontend: Login via Keycloak (OIDC Authorization Code) — 2026-08-22 (lacuna identificada durante `/implement TASK-05.1`, criada e implementada em sequência)
- **Arquivos:** `frontend/src/lib/auth/*` (config, session, token-exchange, proxy-url, return-to, renovacao + testes); `frontend/src/app/api/auth/{login,callback,logout}/route.ts`; `frontend/src/app/api/proxy/[...path]/route.ts`; `frontend/src/proxy.ts` (convenção Next 16, substitui `middleware.ts`); `docker-compose.yml` (env vars OIDC do frontend + `KEYCLOAK_ISSUER_URI_PUBLIC` do backend); `backend/.../config/SecurityConfig.java` (+ `JwtDecoder` bean separando jwk-set-uri interno de issuer público); `backend/.../application.yml` (+ `crudao.keycloak.issuer-uri-interno`/`issuer-uri-publico`)
- **Testes:** 28 testes unitários (vitest) verdes nas libs puras de auth (config, token-exchange, proxy-url, return-to, renovacao — incl. dedupe de refresh concorrente); lint e `next build` limpos; backend 37 testes + build limpo; fluxo completo validado via `docker compose` real (login → Keycloak → callback → sessão em cookie httpOnly → proxy retorna dados reais do backend autenticado → logout limpa sessão)
- **Nota técnica:** achado de infra — no modo `start-dev` do Keycloak, o `iss` do token em fluxos Authorization Code segue o host do `/authorize` (público), não o da troca do `code` (interno); backend agora separa jwk-set-uri (interno) de validação de `iss` (público) — ver TASK-05.0 para detalhes
- **Code review:** agent QA — aprovado com ressalvas; 1 finding 🔴 (open redirect via `returnTo`) e 5 🟡 corrigidos (fallback 401 no refresh falho, dedupe de refresh concorrente, parse seguro do cookie de state, guardrail de log para `COOKIE_SECURE`); guardrails G-AUTH-01 a G-AUTH-04 registrados em `docs/spdd/kanban-configuravel-canvas.md` (dimensão S)
- **Próxima task:** TASK-05.2 — Frontend: Dashboard de gestão (aguardando autorização)

- **Task implementada:** TASK-05.1 — Frontend: Board principal (drag-and-drop, cards, raias) — 2026-08-23
- **Arquivos:** `frontend/src/lib/api/{types,client}.ts`; `frontend/src/lib/board/{transicoes,agrupar,tempo,realtime}.ts` (+ testes); `frontend/src/components/board/{BoardApp,CardTarefa}.tsx` (+ CSS modules); `frontend/src/components/ui/{toast,ModalErro,Skeleton}.tsx`; `frontend/src/app/page.tsx` (board), `frontend/src/app/tarefas/[id]/page.tsx` (detalhe: tempo por etapa, impedimento, observadores); `frontend/src/app/{globals.css,layout.tsx}` (tokens DDR-001, fonte Roboto); `frontend/package.json` (+ `@stomp/stompjs`); `backend/.../domain/workflow/{TransicaoController,TransicaoService}.java` (+ `GET /api/transicoes?workflowId=`, endpoint de leitura que não existia); `backend/.../domain/rbac/{UsuarioController,UsuarioDTO}.java` (novo — `GET /api/usuarios` somente leitura, resolve responsável em nome/inicial no card)
- **Testes:** 43 testes unitários (vitest) verdes — engine de transições (avançar/retroceder/reabertura, RF-012), agrupamento raia×etapa, formatação de duração, libs de auth da TASK-05.0; lint e `next build` limpos; backend `mvn verify` verde
- **Testes manuais via `docker compose` real:** login → board carrega dados reais pelo proxy; `mover` para etapa sem transição válida retorna 409 (server-side); impedimento marca/desmarca e reflete em `registros-etapa`; evento STOMP chega ao subscriber em ~200ms (RNF-001 exige <2s), testado com cliente `@stomp/stompjs` real
- **Nota técnica:** escopo consciente — RF-003 (criação de tarefa) fora do RF desta task, sem UI de criação; Painel de Administração (TASK-05.3) e Dashboard (TASK-05.2) ficam para tasks seguintes
- **Code review:** agent QA — 1 finding 🔴 corrigido (STOMP CONNECT sem autenticação — `StompAuthChannelInterceptor` no backend valida JWT no frame CONNECT; `GET /api/ws-token` no frontend expõe o access_token ao client só para esse fim, exceção documentada à regra geral de token-nunca-no-JS) e 5 🟡 corrigidos (email removido do `UsuarioDTO`, guard de projeto em evento tardio, `onDragEnd` para limpar drag cancelado, fetch redundante removido, feedback de falha de conexão STOMP); guardrails G-RT-01, G-RBAC-05, G-FE-01 registrados no canvas

- **Task implementada:** TASK-05.2 — Frontend: Dashboard de gestão — 2026-08-23
- **Arquivos:** `frontend/src/components/dashboard/{DashboardApp,DashboardApp.module.css}.tsx`; `frontend/src/app/dashboard/page.tsx`; `frontend/src/lib/board/realtime.ts` (+ `conectarDashboard`); `frontend/src/lib/api/types.ts` (+ `DashboardResultado`, `StatusJobDashboard`); `frontend/src/components/board/BoardApp.tsx` (+ link para `/dashboard`)
- **Testes:** sem TDD (task de UI declarativa consumindo endpoints já cobertos por testes de backend, decisão da Fase 1 do `/implement`); `tsc --noEmit`, `eslint` e `next build` limpos; fluxo REST completo validado via `docker compose` real (`POST .../dashboard/calcular` → 202 → polling `GET .../dashboard/jobs/{jobId}` → `CONCLUIDO` com mapas por etapa batendo com `GET /etapas`)
- **Nota técnica:** `DashboardResultadoDTO` do backend não expõe contagem de tarefas por etapa nem "tarefas concluídas no período" — campos do protótipo aprovado (`qtdTarefas`, `tarefasConcluidas`) foram omitidos por não terem dado de origem; renderizado apenas lead-time médio e tempo médio em impedimento por etapa (gráfico de barras + tabela), fiel ao que a API retorna. Fallback de polling ativa somente quando a conexão STOMP falha (`aoFalhar` de `conectarDashboard`), consistente com o padrão de `conectarBoard`.
- **Nota de ambiente:** mesma colisão de `usuario.email` remanescente já registrada na TASK-03.1 reapareceu (dado residual do volume Postgres local, não do código) — removida com autorização do usuário para validar o fluxo limpo via `docker compose`.
- **Code review:** agent QA — 1 finding 🔴 corrigido (corrida entre o cálculo assíncrono, rápido para poucos registros, e o handshake STOMP: `pg_notify` podia publicar antes da subscription se estabelecer e o fallback de polling só disparava em falha de conexão, não nesse caso — `conectarDashboard` ganhou callback `aoConectar`, usado para uma consulta de "catch-up" ao job assim que a subscription é confirmada) e 1 🟡 corrigido (seletor de projeto do dashboard não persistia `crudao_projeto_id` no localStorage, divergindo do Board); demais achados 🟢 confirmados sem ação (ausência de testes de componente é decisão consistente com `BoardApp`/`TarefaDetalhePage`; omissão de `qtdTarefas`/`tarefasConcluidas` do protótipo já documentada como nota técnica por falta de dado de origem no backend)
- **Próxima task:** TASK-05.3 — Frontend: Painel de Administração

- **Sincronização executada:** /spdd-sync — 2026-08-23
- **Divergências encontradas:** 1
- **Resolvidas:** 0 | **Pendentes:** 0 | **Aceitas:** 1
- **Artefato:** docs/spdd/kanban-configuravel-deviations.md

- **Clarificação executada:** /clarify — 2026-08-23
- **Motivo:** implementação da TASK-05.3 (painel de administração) interrompida — AC pedia restrição "usuário edita apenas o projeto de origem", mas o RBAC da TASK-04.1 é só por papel global, sem vínculo usuário↔projeto (nenhum enforcement possível no backend, achado bypassável até por chamada direta à API). Usuário trouxe o modelo de negócio completo de papéis/projeto para resolver a lacuna.
- **Ambiguidades resolvidas:** 13 (Projeto×Board, modelo RBAC por projeto, escopo do project_admin, significado de "finalizar" tarefa, permissão de reabertura, CRUD de tarefa por papel, definição de "tarefa iniciada", configurabilidade via toggles, escopo do log de auditoria, acesso do gestor, finalização de projeto, provisionamento de usuário, permissão de impedimento, papel `user` legado)
- **PRD:** v1.2 — novos RF-015, RF-016, RF-017; RF-003/RF-008/RF-012/RF-013 revisados; RN-008 a RN-016 adicionadas; RNF-003 reforçada
- **Decisão registrada:** BDR-001 (RBAC por projeto com papéis acumuláveis)
- **Artefatos marcados stale:** techspec, data-model, tasks (índice + arquivos), canvas (dimensões A/S/N/O ainda refletem o modelo antigo — só R foi atualizada)
- **Próximo passo:** `/techspec` para redesenhar o modelo de RBAC (Usuario↔Projeto↔Papel, toggles por projeto, permissão `tarefa:finalizar`, auditoria de tarefa) antes de retomar TASK-04.1 (retrabalho) e TASK-05.3

- **Etapa concluída:** /techspec (v1.1) — 2026-08-23
- **Artefatos:** docs/techspec/kanban-configuravel-techspec.md (v1.1) + data-model.md (v1.1) + quickstart.md (v1.0, gerado pela primeira vez) + docs/decisions/ADR-006-rbac-por-projeto-enforcement.md
- **Sistemas afetados:** CRUDAO (único)
- **Mock contracts:** nenhum novo (Keycloak já validado desde a TASK-00.1)
- **Mudanças principais:** novo `ADR-006` (autorização por projeto via `AutorizacaoProjetoService` chamado explicitamente no Service, não mais só `@ExigePermissao`/AOP genérico — `PermissaoAspect` fica restrito a ações globais); data model com `UsuarioProjetoPapel`, `ConfiguracaoProjeto` (toggles), `AuditoriaTarefa`, `Usuario.admin`, `Projeto.data_finalizacao`; novos endpoints (`GET /api/usuarios/me`, membros de projeto, configuração/toggles, finalizar/reabrir projeto, histórico de auditoria, atribuir tarefa); novas permissões `tarefa:atribuir`/`tarefa:finalizar`; matriz de rastreabilidade cobre RF-001 a RF-017 (verificado via `check_rf_coverage.py`, exit 0)
- **Cobertura de RF:** completa (verificação direta do script — o aviso do `validate.py` é bug de argumentos no `validate-rules.json` do skill `techspec`, não relacionado ao conteúdo)
- **Próximo comando:** `/tasks kanban-configuravel` — vai precisar gerar uma task de retrabalho da TASK-04.1 (RBAC) antes de a TASK-05.3 (painel de administração) poder ser reimplementada

- **Comitê de análise assíncrono executado:** security + database + architect — 2026-08-23 (sobre TechSpec v1.1)
- **Achado crítico (security):** contrato de `PapelController` estava implícito e deixava aberto um vetor de escalação de privilégio (`project_admin` manipulando permissões de um papel existente, dado que `papel:gerenciar` era uma das 6 chaves candidatas a `UsuarioProjetoPapel`). **Corrigido:** `papel:gerenciar` nunca é atribuível via papel de projeto — checada só contra `Usuario.admin`; RN-006 do PRD marcada como superseded em parte (PRD v1.3, G-RBAC-07 no canvas)
- **Achado convergente (security + architect):** troca do AOP genérico por chamada explícita (ADR-006) perde garantia "por construção" — mitigado com teste estrutural de CI verificando a chamada em todo Service de escrita escopado a projeto (G-RBAC-06)
- **Achado (architect):** checagem de projeto finalizado (RN-015) deve ser unificada dentro de `AutorizacaoProjetoService`, não duplicada por Service (G-RBAC-08); recomendado TASK-04.2 nova (não reabrir TASK-04.1 in-place) para o retrabalho, quebrada em 5 subtarefas (Q-009)
- **Achados (database):** estratégia de migração de `Usuario.papel_id` resolvida (Q-006 — reatribuição manual, sem herdar escopo implícito); seed padrão de `project_admin` resolvido (Q-007 — todas as chaves exceto `papel:gerenciar`); índices `idx_upp_projeto` e `idx_auditoria_tarefa` adicionados; PK de `UsuarioProjetoPapel` com ordem explícita; retenção de `AuditoriaTarefa` registrada como Q-008 (não bloqueante); risco de N+1 em `GET /usuarios/me` documentado
- **Artefatos atualizados:** PRD v1.3, TechSpec v1.2, data-model v1.2, ADR-006 revisado, canvas (guardrails G-RBAC-06/07/08)
- **Próximo comando:** `/tasks kanban-configuravel`

- **Etapa concluída:** /tasks (v1.1 — atualização, não regeneração) — 2026-08-23
- **Artefato:** docs/tasks/kanban-configuravel-tasks.md
- **Tasks novas:** TASK-04.2 (RBAC por projeto — retrabalho, EPIC-04), TASK-01.3 (toggles + finalização de projeto, EPIC-01), TASK-02.3 (regras avançadas de tarefa + auditoria, EPIC-02), TASK-05.4 (ajustes de UI de tarefa, EPIC-05)
- **Tasks retrabalhadas:** TASK-05.3 (dependências: TASK-04.1→TASK-04.2, TASK-01.2, +TASK-01.3; escopo expandido — membros de projeto, toggles, finalizar, papéis só admin global); TASK-06.1 (+dependência TASK-05.4)
- **Tasks preservadas sem alteração de conteúdo:** TASK-00.1, 00.2, 01.1, 01.2, 02.1, 02.2, 03.1, 05.0, 05.1, 05.2 (já concluídas) — TASK-04.1 recebeu só nota de retrabalho no topo, apontando para TASK-04.2
- **Total:** 16 tasks em 7 epics (12 originais + 4 novas)
- **Canvas:** transitou para **READY** — todas as 7 dimensões preenchidas (R, E, A, Structure, N, Safeguards, O)
- **Próxima task recomendada:** `/implement TASK-04.2` (RBAC por projeto — bloqueia TASK-01.3, TASK-02.3 e TASK-05.3)

- **Análise executada:** /analyze — 2026-08-23
- **Findings:** 🔴 2 | 🟡 2 | 🟠 1 | 🔵 1
- **Veredicto:** ⚠️ Aprovado com ressalvas
- **Artefato:** docs/analyze/kanban-configuravel-analysis.md
- **RFs cobertos:** 17/17 (100%)

- **Remediação aplicada:** 2026-08-23 — todos os 4 findings 🔴/🟡 corrigidos
  - G1: TASK-04.2 ampliada — migração de `@ExigePermissao`→`AutorizacaoProjetoService` agora inclui os 7 endpoints de `TarefaController` (antes só listava Workflow/Etapa/Transição/Raia)
  - G2: TechSpec → **v1.3** — contrato explícito de `PATCH /api/tarefas/{id}/mover-projeto`, exigindo permissão nos dois projetos (origem e destino); item correspondente adicionado à TASK-04.2
  - S1/S2: tags RN-008, RN-014, RN-013 adicionadas ao checklist/critérios de aceite da TASK-04.2
  - `docs/analyze/kanban-configuravel-analysis.md` atualizado — veredicto final **✅ Aprovado para implementação**
  - M1/B1 (🟠/🔵) não corrigidos — não bloqueantes, decisão registrada no próprio finding

- **Task implementada:** TASK-04.2 — RBAC por projeto: retrabalho do modelo e enforcement — 2026-08-24
- **Arquivos:** `domain/rbac/{UsuarioProjetoPapel,UsuarioProjetoPapelId,UsuarioProjetoPapelRepository,ProjetoPapeisDTO,UsuarioMeDTO,MembroDTO,AtribuirPapeisRequest,MembroProjetoService,MembroProjetoController,MigracaoAdminRunner}.java` (novos); `domain/rbac/{Usuario,RbacSeeder,UsuarioController}.java`, `domain/projeto/{Projeto,ProjetoService}.java` (+ novo `ConfiguracaoProjeto`/`ConfiguracaoProjetoRepository`), `security/{UsuarioContexto,PermissaoAspect}.java` (+ novo `AutorizacaoProjetoService`), `common/{ApiExceptionHandler,EntradaInvalidaException}.java` (novo, 422); migração de `@ExigePermissao` para chamada explícita em `WorkflowService`, `EtapaService`, `TransicaoService`, `RaiaService`, `TarefaService` (7 métodos, incl. `mover-projeto` com 2 checagens — finding G2) e remoção da anotação dos respectivos Controllers; teste estrutural `architecture/AutorizacaoProjetoEnforcementTest`
- **Testes:** TDD nos pontos críticos (`AutorizacaoProjetoServiceTest` 7 casos incl. RN-015 e exceção de reabertura, `MembroProjetoServiceTest` 5 casos incl. 422/G-RBAC-07, `RbacSeederTest` 4 casos incl. RN-013); suíte completa 57/57 verde (`mvn test`, unit only — `*IT` não executados nesta sessão, mesma limitação de rede Docker-in-Docker já registrada na TASK-04.1); `spotless:check` limpo
- **Nota técnica:** reabertura/finalização de projeto (endpoint) fica para TASK-01.3 — `AutorizacaoProjetoService` já implementa a exceção de RN-015 para a permissão `projeto:gerenciar`, pronta para esse endpoint. Raia default global (`projetoId=null`) gerenciada só por `admin` global (sem projeto para escopar). `Usuario.papel`/`papel_id` mantidos como `@Deprecated` (não removidos — migration de schema separada, Q-006)
- **Débito técnico registrado (não bloqueante):** extensão de `StompAuthChannelInterceptor` para restringir subscription STOMP a membros do projeto (G-RT-01), agora que `UsuarioProjetoPapel` existe
- **Code review:** agent QA (general-purpose, agente `qa` indisponível no registro desta sessão) — 1 finding 🔴 corrigido: `ProjetoController`/`ProjetoService` haviam ficado fora da migração (decisão original desta task, por não constarem na checklist), mas o `RbacSeeder` desta mesma task passou a dar `projeto:gerenciar` a `project_admin` — como o `@ExigePermissao` legado checa `Usuario.papel` (papel único global, não escopado), isso permitia a um `project_admin` gerenciar **qualquer** projeto do sistema, não só o seu (violação de RF-015/BDR-001 introduzida pela própria mudança de seed). Corrigido: `editar`/`excluir`/`workflow-ativo` migrados para `AutorizacaoProjetoService` (escopados); `criar` (ação global, sem projeto ainda) restrito a `Usuario.admin`, mesmo padrão de `papel:gerenciar`. 2 findings 🟡 confirmados sem ação (exceção de `projeto:gerenciar` ao bloqueio RN-015 é código morto até TASK-01.3 expor o endpoint de reabertura — reavaliar então; teste estrutural G-RBAC-06 verifica presença da chamada por texto, não o efeito — suficiente para o objetivo de "pegar remoção acidental", limitação documentada no próprio teste)
- **Próxima task:** TASK-01.3 (toggles + finalização de projeto) e TASK-02.3/TASK-05.3, agora desbloqueadas

- **Task implementada:** TASK-01.3 — Configuração de projeto (toggles) e finalização — 2026-08-24
- **Arquivos:** `domain/projeto/{ConfiguracaoProjetoDTO,ProjetoDTO,ProjetoService,ProjetoController}.java` (novos endpoints `GET/PUT /{id}/configuracao`, `PUT/DELETE /{id}/finalizar`); `security/AutorizacaoProjetoService.java` (novo método `exigirPermissaoParaReabertura`, desacoplado de `exigirPermissao`); testes `domain/projeto/ProjetoServiceTest.java` (novo), `security/AutorizacaoProjetoServiceTest.java` (+3 casos), `architecture/AutorizacaoProjetoEnforcementTest.java` (G-RBAC-06 ampliado)
- **Testes:** TDD (`ProjetoServiceTest`, 6 casos incl. isolamento de toggle por projeto e reabertura); suíte completa 64+ testes unit verde (`mvn test`, unit only); `spotless:check` limpo
- **Achado técnico (código pré-existente da TASK-04.2, exposto por esta task):** `AutorizacaoProjetoService.exigirPermissao` usava a própria string de permissão (`"projeto:gerenciar"`) como sinalizador de "pode reabrir projeto finalizado" — como `editar`/`excluir`/`definirWorkflowAtivo` do `ProjetoService` já usavam essa mesma chave, RN-015 nunca bloqueava esses três métodos (bug real, não só dos novos endpoints). Corrigido: `exigirPermissaoParaReabertura` é um método dedicado, chamado só por `ProjetoService.reabrir` — `exigirPermissao` geral agora bloqueia incondicionalmente projeto finalizado, sem exceção por string de permissão
- **Débito técnico registrado (não bloqueante):** falta teste `*IT` cruzando `ProjetoService` real + `AutorizacaoProjetoService` real (hoje cobertos separadamente por unit tests) — mitigação suficiente para o escopo atual, mas recomendável antes de expandir `AutorizacaoProjetoService`
- **Code review:** agent QA (general-purpose) — 1 finding 🔴 corrigido (acima), 2 🟡 confirmados sem ação corretiva imediata
- **Próxima task:** TASK-02.3 (regras avançadas de tarefa + auditoria) ou TASK-05.3 (painel de administração)

- **Task implementada:** TASK-05.3 — Frontend: Painel de Administração — 2026-08-24
- **Arquivos:** `frontend/src/lib/api/types.ts` (+ `ConfiguracaoProjeto`, `ProjetoPapeis`, `UsuarioMe`, `Membro`, `Papel`; `Projeto.dataFinalizacao`); `frontend/src/components/admin/{AdminApp,AdminApp.module.css}.tsx` (novo — container, gating via `GET /usuarios/me`, seletor de projeto compartilhando `crudao_projeto_id` com o board, abas); `frontend/src/components/admin/abas/{ProjetosAba,WorkflowsAba,RaiasAba,MembrosAba,TogglesAba,PapeisAba}.tsx` (novos); `frontend/src/app/admin/page.tsx` (novo); `frontend/src/components/board/BoardApp.tsx` (+ link "Configurações do projeto →"). Só frontend — todos os endpoints consumidos já existiam (TASK-04.2/01.3/02.3/04.1)
- **Testes:** sem TDD (UI declarativa consumindo endpoints já cobertos por teste de backend, mesma decisão da TASK-05.2); `tsc --noEmit`, `eslint` e `next build` limpos
- **Nota técnica:** gating de UI (`admin`/`projetos[].permissoes` de `/usuarios/me`) é só estético — nenhuma escrita depende de dado do cliente para decidir autorização (RNF-003/ADR-006, backend revalida tudo); aba "Papéis e permissões" só renderiza com `admin===true`; `MembrosAba` filtra `papel:gerenciar` por conteúdo de permissão (não por nome do papel), robusto a rename (G-RBAC-07)
- **Code review:** agent QA (general-purpose) — aprovado sem findings 🔴/🟡; 2 findings 🔵 (não bloqueantes) — S1 (nota de manutenibilidade sobre raias globais, sem ação) e S2 (`AdminApp.permissoesProjeto` sintetizava um Set fixo de chaves para `admin` em vez de checagem direta) **corrigido nesta sessão** — guardrail **G-FE-02** registrado no canvas (dimensão S)
- **Próxima task:** TASK-05.4 (ajustes de UI de tarefa) ou TASK-06.1 (testes E2E de fechamento)

- **Task implementada:** TASK-02.3 — Regras avançadas de tarefa: edição travada, atribuição, finalização e auditoria — 2026-08-24
- **Arquivos:** `domain/tarefa/{AuditoriaTarefa,CampoAuditoria,AuditoriaTarefaRepository,AuditoriaTarefaDTO,AtribuirResponsavelRequest,Tarefa,TarefaService,TarefaController}.java`; `security/AutorizacaoProjetoService.java` (+ `temPermissao`, `usuarioTemAcessoAoProjeto`, `exigirProjetoNaoFinalizado`)
- **Testes:** TDD (`TarefaServiceTest`, 18 casos incl. trava de edição RN-009/010, `tarefa:finalizar` RN-011 na ida/volta, autoatribuição RN-012, auditoria RN-016, projeto finalizado bloqueando "puxar"); suíte completa 74/74 verde (`mvn test`, unit only); `spotless:check` limpo
- **Nota técnica:** "dev-tier" (papel `dev`) é inferido pela ausência de `tarefa:atribuir` — único papel com `tarefa:gerenciar` que não tem essa permissão entre os seedados (`RbacSeeder`), evita checagem por nome de papel; `iniciada` persistida como boolean em `Tarefa`, marcada ao sair da etapa de menor `ordem` do workflow pela 1ª vez; `PUT /tarefas/{id}` (edição geral, TASK-02.1) continua permitindo alterar `responsavelId` sem passar pela checagem RN-012 do novo `PATCH /responsavel` — decisão consciente de escopo
- **Code review:** agent QA — aprovado com ressalvas; 2 findings 🟡 corrigidos (checagem de RN-015 duplicada fora do ponto único `AutorizacaoProjetoService` — extraído `exigirProjetoNaoFinalizado` público, G-RBAC-08; teste de cobertura adicionado para RN-015 no fluxo de "puxar" tarefa); 1 finding 🟢 registrado sem ação (atribuir a terceiro não valida se o usuário de destino é membro do projeto — não bloqueante)
- **Próxima task:** TASK-05.3 (painel de administração) ou TASK-05.4 (ajustes de UI de tarefa)

- **Task implementada:** TASK-05.4 — Frontend: ajustes de tarefa para RBAC por projeto — 2026-08-24
- **Arquivos:** `backend/.../domain/tarefa/TarefaDTO.java` (+ campo `iniciada`, auto-mapeado por MapStruct); `frontend/src/lib/api/{types,client}.ts` (+ `AuditoriaTarefa`/`CampoAuditoria`, parse do corpo `{erro}` de erros HTTP para mensagem legível em vez do JSON bruto — corrige todos os fluxos de erro do app, não só desta task); `frontend/src/app/tarefas/[id]/page.tsx` (reescrito — edição de título/descrição com trava por papel "dev-tier" + toggle `devPodeEditarTarefaIniciada`, "Atribuir a mim" sempre visível + reatribuir gated por `tarefa:atribuir`, seção de histórico via `GET /historico`); `frontend/src/lib/board/agrupar.test.ts` (fixture + `iniciada`)
- **Testes:** sem TDD (ajustes de UI consumindo endpoints já cobertos por testes de backend das TASK-02.3/04.2); `tsc --noEmit`, `eslint` e `vitest run` (43/43) limpos; backend não compilado nesta sessão (mesma limitação de rede/Docker já registrada — mudança é campo record auto-mapeado, mesmo padrão de `impedida`)
- **Nota técnica:** heurística "dev-tier" (tem `tarefa:gerenciar` mas não `tarefa:atribuir`) no frontend é só gating de UX, mesma lógica do backend (`TarefaService.ehDevTier`) — backend é a fonte real de autorização (RNF-003)
- **Code review:** agent QA (general-purpose) — aprovado sem findings bloqueantes; 2 🟡 não corrigidos (botão "Atribuir a mim" não checa vínculo ao projeto antes de exibir — backend já rejeita corretamente; `key={i}` na tabela de histórico por falta de id no DTO)
- **Próxima task:** TASK-05.3 (painel de administração — arquivos já presentes no working tree, não commitados/registrados aqui) ou TASK-06.1 (testes E2E)

- **Verificação:** TASK-05.3 — Frontend: Painel de Administração — confirmada como já implementada e concluída (arquivo individual marcado "Concluída — 2026-08-24"; `frontend/src/app/admin/`, `frontend/src/components/admin/`, `backend/.../MigracaoAdminRunner.java` presentes no working tree, ainda não commitados) — verificação feita ao iniciar a TASK-06.1, que dependia dela

- **Task implementada:** TASK-06.1 — Testes E2E dos fluxos principais e revisão de cobertura — 2026-08-24
- **Arquivos:** `frontend/playwright.config.ts` (novo); `frontend/e2e/{fixtures/api.ts,fixtures/login.ts,board.spec.ts,dashboard.spec.ts,rbac.spec.ts}` (novos, 14 testes); `frontend/vitest.config.ts` (+ `exclude: ['e2e/**']`, evita que o vitest tente rodar specs do Playwright); `frontend/src/components/board/{CardTarefa,BoardApp}.tsx` (+ `data-testid` em card/menu/célula — únicos hooks de teste que faltavam para seletores E2E estáveis, sem mudança de comportamento)
- **Ferramenta escolhida (Q-005):** Playwright — specs rodam contra a stack real via `docker compose up` (não sobem/derrubam a stack sozinhas)
- **Testes:** 14 testes E2E (Playwright) cobrindo RF-001/002/004/005/012 (board: mover via menu e via drag-and-drop, 409 em transição inválida, impedimento, desfinalizar/REABERTURA, tempo real STOMP ≤2s), RF-006/RF-007 (dashboard assíncrono), e RBAC por projeto (bloqueio de ação sem permissão, isolamento entre projetos, autoatribuição RN-012, `tarefa:finalizar` na ida e na volta RN-011, projeto finalizado bloqueando escrita RN-015, toggles RF-016 travando edição, aba "Papéis" só admin global) — todos passando contra `docker compose up` real; suíte unitária do frontend (43 testes) e do backend (`mvn test` + `spotless:check`) revalidadas, sem regressão; `next build` e `tsc --noEmit` limpos
- **Setup de fixtures:** specs usam a API do backend autenticada via Keycloak (password grant, `directAccessGrantsEnabled` do realm de dev) para montar cada cenário (projeto/workflow/etapas/transições/raia/tarefas/membros) — cada teste cria seu próprio projeto, evitando interferência entre testes paralelos; a UI só é exercitada nos pontos que o critério de aceite pede
- **Achado de ambiente corrigido:** volume local do PostgreSQL estava desatualizado (schema anterior à TASK-01.3/02.3 — `ddl-auto=update` não conseguia adicionar `tarefa.iniciada`/`usuario.admin` como `NOT NULL` em tabelas com linhas existentes); resetado (`docker compose down -v`) com autorização do usuário, mesmo padrão de residual já registrado nas notas de ambiente das TASK-03.1/05.2
- **Cobertura vs. metas de testing.md:** TDD amplo nos Services de domínio (visível pelo histórico de tasks — cada regra de negócio relevante tem teste unitário dedicado); não há ferramenta de medição de cobertura configurada no backend (sem JaCoCo) nem no frontend — meta numérica (80% TDD / 100% BDD) não é verificável automaticamente hoje. Não bloqueante para esta task (revisão qualitativa feita), registrado como débito técnico não bloqueante para eventual task futura de observabilidade/CI.
- **Canvas:** `docs/spdd/kanban-configuravel-canvas.md` já estava `READY` (7/7 dimensões) desde 2026-08-23 — confirmado sem necessidade de alteração
- **Code review:** TASK-06.1 — APROVADO COM RESSALVAS — 2026-08-24
- **Findings:** 0 críticos, 2 importantes (I1 meta de cobertura sem tooling de medição — JaCoCo ausente; I2 race condition real em `UsuarioContexto.provisionar` sob login concorrente, achada pela suíte E2E, mitigada no fixture `global-setup.ts`, correção na origem fica para task futura), 2 sugestões (S1 aplicado — seletores por `data-testid` em vez de `.locator('..')`; S2 avaliado — credenciais de dev duplicadas em `e2e/fixtures/api.ts`, mesmo valor já público em `crudao-realm.json`)
- **Arquivos ajustados no review:** `frontend/e2e/global-setup.ts` (novo — provisiona admin.teste/user.teste serialmente antes dos workers paralelos), `frontend/playwright.config.ts` (+ `globalSetup`), `frontend/e2e/{board,rbac}.spec.ts` (seletores por `data-testid` — finding S1)
- **Artefato:** `docs/checklists/kanban-configuravel-TASK-06.1-review.md`
- **Canvas:** dimensão S atualizada (guardrails G-RBAC-09, G-TEST-01) — permanece `READY`
- **Próximo passo:** abrir task de bug-fix para I2 (race condition em `UsuarioContexto.provisionar`) quando conveniente; feature pronta para merge

- **Bug-fix (achado em teste manual do usuário):** admin não conseguia criar o primeiro projeto pela UI — 2026-08-24
- **Causa:** `AdminApp.tsx` só renderizava `ProjetosAba` (onde vive o formulário "Novo projeto") quando `projetoAtual` já existia; sem nenhum projeto cadastrado, `projetoAtual` é sempre `null`, então a aba "Projeto" ficava em branco — nenhum caminho de UI para criar o primeiro projeto (só via API direta)
- **Arquivos:** `frontend/src/components/admin/AdminApp.tsx` (renderiza `ProjetosAba` também quando `projetos.length === 0 && admin`), `frontend/src/components/admin/abas/ProjetosAba.tsx` (`projeto` agora `Projeto | null` — seção "Dados do projeto" só aparece com projeto existente; "Novo projeto" sempre visível para admin); `frontend/src/components/board/BoardApp.tsx` (link "Ir para Configurações →" no estado vazio "Nenhum projeto cadastrado ainda.", achado numa interação anterior do mesmo teste manual)
- **Validação:** `tsc`/`eslint` limpos; smoke test Playwright confirmou o fluxo completo (login admin → `/admin` sem projetos → "Novo projeto" visível → criar → toast de sucesso); suíte E2E completa (14/14) revalidada sem regressão após a mudança
- **Nota técnica:** este bug não tinha sido pego pela suíte de TASK-05.3/06.1 porque todos os specs criam o cenário (projeto) via API antes de abrir a UI — nenhum teste exercitava o estado "admin, zero projetos, só UI"
- **Feature kanban-configuravel:** todas as 16 tasks do plano (00.1 a 06.1) concluídas e revisadas

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
| docs/prd/kanban-configuravel-prd.md | 1.3 | ok |
| docs/techspec/kanban-configuravel-techspec.md | 1.3 | ok |
| docs/techspec/kanban-configuravel/data-model.md | 1.2 | ok |
| docs/techspec/kanban-configuravel/quickstart.md | 1.0 | ok (gerado na revisão v1.1 — não existia desde o v1.0) |
| docs/tasks/kanban-configuravel-tasks.md (índice) | 1.1 | ok |
| docs/tasks/kanban-configuravel/ (16 arquivos TASK-*.md — 12 originais + TASK-01.3, 02.3, 04.2, 05.4 novas) | 1.1 | ok |
| docs/spdd/kanban-configuravel-canvas.md | — | **READY** (7/7 dimensões preenchidas) |
| docs/analyze/kanban-configuravel-analysis.md | 1.0 | ✅ aprovado para implementação — 4 findings 🔴/🟡 corrigidos |
| docs/contracts/CRUDAO-keycloak-contract.md | 1.0 | ok (validado na TASK-00.1) |
| docs/design/kanban-configuravel-design-brief.md | 1.0 | ok |
| docs/design/prototypes/kanban-configuravel/ (fontes + Artifact https://claude.ai/code/artifact/a7612319-88d0-434d-9729-64d3d1604c6c) | — | aprovado pelo usuário em 2026-08-22 |
| docs/tasks/kanban-configuravel-tasks.md (índice) | 1.0 | ok |
| docs/tasks/kanban-configuravel/ (12 arquivos TASK-*.md) | 1.0 | ok |
| docs/spdd/kanban-configuravel-canvas.md | — | draft (R, E, A, S, N, O preenchidas; falta Safeguards — aguarda /code-review) |
| docs/spdd/kanban-configuravel-deviations.md | 1.0 | ok |
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
