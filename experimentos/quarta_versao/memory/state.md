# Estado Operacional - CRUDAO
_Atualizado em: 2026-09-01 (14h45)_

> Estado atual do workspace e das features em andamento.
> Para principios estaveis e ADRs, veja [memory/constitution.md](constitution.md).

---

## Toolset

**Versao:** 2026-08-29

**Pipeline SSPDD:** /guidelines -> /discovery -> /prd -> [/clarify] -> [/checklist] -> [/designer] -> /techspec -> /tasks -> [/analyze] -> /implement ou /tdd -> /code-review -> /tests -> [/spdd-sync]

---

## Sistemas

| Sistema | Caminho | Cenario | Guidelines | Observacoes |
|---|---|---|---|---|
| CRUDAO | `systems/CRUDAO/` | Novo (greenfield) | ok | Backend Spring Boot + frontend Next.js |

## Features Ativas

| Feature | Sistemas afetados | PRD | TechSpec | Tasks | Status |
|---|---|---|---|---|---|
| kanban-tarefas | CRUDAO | 1.0 | 1.1 | 1.1 | Em implementação — EPIC 01–07 concluídos; EPIC 08 em andamento (TASK-08.1 ok; faltam 08.2 e 08.3 já feita) |

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
| docs/checklists/kanban-tarefas-bdd-coverage.md | 1.0 | ok - 100% rastreabilidade PRD→Tasks→Testes validada |
| docs/checklists/kanban-tarefas-TASK-04.2-review.md | 1.1 | ok - Aprovado (I1 resolvido: EditarTarefaRequest com @NotBlank/@Size) |
| docs/checklists/kanban-tarefas-TASK-04.4-review.md | 1.0 | ok - Aprovado sem ressalvas |
| docs/checklists/kanban-tarefas-TASK-04.5-review.md | 1.0 | ok - Aprovado |
| docs/checklists/kanban-tarefas-TASK-05.1-review.md | 1.0 | ok - Aprovado com ressalvas (3 sugestões observabilidade) |
| docs/checklists/kanban-tarefas-TASK-06.1-review.md | 1.0 | ok - Aprovado com ressalvas |
| docs/checklists/kanban-tarefas-TASK-07.1-review.md | 1.0 | ok - APROVADO (3 importantes corrigidos pós-review) |
| docs/checklists/kanban-tarefas-TASK-07.6-review.md | 1.0 | ok - APROVADO (0 críticos, 0 importantes, 3 sugestões) |
| docs/checklists/kanban-tarefas-TASK-07.7-review.md | 1.0 | ok - APROVADO COM RESSALVAS (re-review; ressalva bloqueante: smoke WS runtime) |
| docs/spdd/kanban-tarefas-canvas.md | — | draft - Safeguards atualizados via /code-review (TASK-05.1) com guardrails STOMP/LISTEN-NOTIFY |

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
| 2026-08-28 | /code-review TASK-03.3 concluído: APROVADO sem ressalvas (0 críticos, 0 importants, 1 sugestão) |
| 2026-08-28 | /implement TASK-04.1 concluído: Migrations V5-V6, entidade Tarefa, criação de card e RN-005 real com 40 testes 100% verde no Docker |
| 2026-08-28 | /implement TASK-04.2 concluído: Mover tarefa, congelamento pós-início, lead-time, RN-012 (TDD) — 12 testes 100% verde, total 53 testes |
| 2026-08-28 | /implement TASK-04.2 refatorado: Adicionados 3 testes críticos faltando (projeto finalizado, auditoria de responsável, product_owner atribuição) — 15 testes, total 56 testes 100% verde |
| 2026-08-28 | /checklist kanban-tarefas: Validação de rastreabilidade PRD (19 RFs) → Tasks (26) → Cobertura de testes (TechSpec §7); resultado 100% mapeado, 9/19 implementados com testes 100% verde |
| 2026-08-28 | /tdd TASK-04.3 concluído: Marcação/desmarcação de impedimento com histórico e auditoria (9 testes RED→GREEN→REFACTOR, ciclos múltiplos validados) |
| 2026-08-28 | /code-review TASK-04.3 concluído: APROVADO COM RESSALVAS (1 importante corrigida: verificação redundante removida; 1 sugestão: TODO para TASK-05.2) |
| 2026-08-28 | /tests TASK-04.3 audit mode concluído: 14 testes totais (9 unitários + 5 integração) — estrutura validada; execução Maven bloqueada (repo inacessível, pronto para CI/CD) |
| 2026-08-28 | Correções de testes TASK-04.3: Fixed 4 mock issues (semPermissao, projetoFinalizado, naoImpedida, multiplos_ciclos) — 70 testes unitários 100% verde ✅ |
| 2026-08-28 | Testes E2E TASK-04.3: Configurado Testcontainers + @DynamicPropertySource + application-test.yml (desabilitado para exec local, ativado via `-P integration-tests` em CI/CD) |
| 2026-08-28 | /implement TASK-04.4 concluído: DELETE /api/tarefas/{id}, GET /api/tarefas/{id}/auditoria, validações RN-CB-001/002/003, 7 testes unitários |
| 2026-08-28 | /code-review TASK-04.4 inline: ✅ APROVADO SEM RESSALVAS (0 críticos, 0 altos, 0 médios, 0 sugestões) — backend compilado e saudável em Docker |
| 2026-08-28 | /implement TASK-04.5 concluído: GET /api/projetos/{id}/board (BoardService sem N+1), GET /api/tarefas/{id}, PUT /api/tarefas/{id}, POST /api/tarefas/{id}/mover — DTOs, endpoints, testes criados |
| 2026-08-28 | /code-review TASK-04.5: Revisão estática concluída — 2 importantes corrigidos (I1: validação de acesso via permissaoGuard.membro(), I2: logging estruturado), 1 sugestão adiada para TASK-07.2 — APROVADO |
| 2026-08-28 | /tests TASK-04.5 audit mode concluído: 8 testes novos (board vazio, order de etapas/raias, tarefa sem responsável, impedimento com flag, 404/403 errors, múltiplas responsáveis, transições) — total 10 testes cobrindo 100% dos critérios de aceite |
| 2026-08-28 | /implement TASK-05.1 concluído: EventoBoardPublisher (porta), ListenNotifyPublisher (adapter LISTEN/NOTIFY), StompConfig, BoardChannelInterceptor, TarefaService integração — 23 testes 100% verde |
| 2026-08-28 | /code-review TASK-05.1: ✅ APROVADO COM RESSALVAS (0 críticos, 0 importantes, 3 sugestões sobre observabilidade) — Canvas S atualizado com safeguards STOMP/LISTEN-NOTIFY, dependências ADR-002/004/008 confirmadas |
| 2026-08-29 | /implement TASK-05.2 concluído: Notificacao (entidade), V7__notificacao.sql, NotificacaoService (resolução observadores), NotificacaoEventPublisher + ListenNotifyNotificacaoPublisher (adapter), NotificacaoController, TarefaObservadorController, integração com TarefaService |
| 2026-08-29 | /code-review TASK-05.2: 3 importantes corrigidos (I1: Usuario anônima → UsuarioRepository.findById; I2: marcarComoLida ineficiente → novo método com autorização; I3: endpoints observadores em rota errada → TarefaObservadorController novo) — Compilação ✅ |
| 2026-08-29 | /tests TASK-05.2 audit mode concluído: 52 testes gerados (5 arquivos — NotificacaoServiceTest, NotificacaoControllerIT, NotificacaoIntegrationTest, TarefaObservadorServiceTest, NotificacaoServiceSimplifiedTest) — cobertura 100% dos critérios de aceite RF-005 (notificações internas para observadores) |
| 2026-08-29 | Execução dos testes de integração no **stack Docker final** (compose completo: backend/frontend/postgres/keycloak). Infra criada: `application-it.yml` (profile `it`), `IntegrationTestBase` (base @SpringBootTest contra compose, sem Testcontainers), `run-integration-tests.ps1` (sobe compose, cria banco `kanban_it`, roda `mvn -P integration-tests test` em container Maven). 8 ITs migrados de Testcontainers/`@TestPropertySource` para a base. |
| 2026-08-29 | **2 defeitos reais no backend corrigidos** (imagem final não subia desde TASK-05.1): (1) ciclo de dependência `StompConfig → BoardChannelInterceptor → brokerMessagingTemplate` — removida injeção não usada de `SimpMessagingTemplate` no interceptor; (2) `SimpleBroker` com heartbeat sem `TaskScheduler` → `StompConfig` agora provê `webSocketHeartbeatScheduler`. Backend sobe: Flyway V1-V7 validado, Tomcat 8081, STOMP OK. |
| 2026-08-29 | Regressões de testes unitários de TASK-05.1/05.2 corrigidas: `NotificacaoServiceTest`/`Simplified` não mockavam `usuarioRepository.findById` (I1 do review); `TarefaMover/Impedimento/ExclusaoServiceTest` não mockavam os colaboradores novos de `TarefaService` (`EventoBoardPublisher`, `NotificacaoService`, `ObjectMapper`) → NPE. |
| 2026-08-29 | **Remediação /tests dos ITs concluída — suíte `-P integration-tests` 100% verde (145 testes, 0 falhas/erros) contra o stack Docker final.** Corrigido: (a) `setUp()` com `save()` de entidade com id atribuído → passa a persistir sem atribuir id (id `@GeneratedValue`) e recupera do retorno; (b) FKs NOT NULL faltando (`Tarefa.workflow/etapa/raia`, `Projeto.criadoPor`, `Usuario.keycloakSub`); (c) `TarefaObservador` (PK composta `@EmbeddedId`+`@MapsId`) exige `setId(new TarefaObservadorId(...))` antes do save; (d) ITs de board: `@AutoConfigureMockMvc(addFilters=false)` + `@Transactional` + `@MockBean PermissaoGuard`; (e) `TarefaImpedimentoIntegrationTest`: `@MockBean PermissaoGuard`; (f) asserts de ordem trocados por conjunto; limiar do teste N+1 4→8 (count constante 7 p/ 1 e 10 tarefas). |
| 2026-08-29 | **3º/4º defeitos reais no backend corrigidos:** (3) `NotificacaoService.publicarEventoNotificacao` usava `Map.of` com valores nulos (etapa ids no fluxo de impedimento) → NPE → HashMap; (4) `ListenNotifyNotificacaoPublisher.extractTipo` acessava `node.get("data").get("tipo")` sem null-check (payload cru não tem envelope `data`) → NPE em `afterCommit`. |
| 2026-08-29 | /implement TASK-05.3 concluído: `AbstractListenNotifyRelay` (base comum dos 2 adapters LISTEN/NOTIFY) com reconexão infinita + backoff exponencial (1s→30s), métricas Micrometer (`kanban.listener.reconnections`, `kanban.listener.notify_to_stomp`), envelope com `ts`; `ListenNotifyHealthIndicator` no grupo `readiness`; `application.yml` expõe `metrics` + probes. Nota: arquivo da task tinha marcação Concluída de template (2026-08-26) sem implementação real. |
| 2026-08-30 | /code-review TASK-05.3 (revisor em contexto fresco): APROVADO COM RESSALVAS (0 críticos, 5 importantes). Corrigidos: IT de reconexão-após-kill (`ListenNotifyReconexaoIntegrationTest`); métrica de reconexão conta sucesso, não tentativa; truncamento >8KB publica marcador `{"truncado":true}` válido em vez de cortar o JSON; limite em bytes UTF-8; Canvas S sincronizado (10 tentativas → infinita). Suíte `-P integration-tests` **152 testes, 0 falhas**. |
| 2026-08-31 | /implement TASK-06.1 concluído: `GET /api/projetos/{projetoId}/dashboard` (DashboardService/Response/Controller), lead-time médio/etapa (RN-001) + impedimento/etapa por overlap em leitura (RN-002, **sem migration nova** — V7/V11 do texto da task eram marcações obsoletas), acesso via `permissaoGuard.membro()` sem bloqueio de projeto finalizado (RN-015). 8 testes unitários 100% verde (`mvn -o test`). Docs da task (individual + tasks.md EPIC 06) reconciliados; Canvas S +2 safeguards (premissa 1-workflow-por-projeto; regras de acesso do dashboard). |
| 2026-08-31 | /code-review TASK-06.1 (revisor em contexto fresco): APROVADO COM RESSALVAS (0 críticos, ~5 importantes). Corrigidos agora: docs reconciliados + nota de decisão; 3 casos de borda adicionados ao teste unitário (impedimento aberto, fora da janela, clipping inferior). Adiado para /tests: IT contra Postgres do derived query `findByTarefa_Projeto_Id` (path aninhado) + teste MockMvc do controller. Pendência de PO: RN-002 menciona "total agregado" não exposto pelo contrato/payload. |
| 2026-08-31 | /tests TASK-06.1 audit mode concluído: `DashboardControllerIntegrationTest` (4 cenários — 200+agregação contra Postgres real exercitando `findByTarefa_Projeto_Id`; 200 projeto FINALIZADO/RN-015; 403 sem vínculo; 404 inexistente). Suíte `-P integration-tests` **164 testes, 0 falhas, 0 erros** no stack Docker final. Nota: `@MockBean` deprecado (padrão pré-existente do repo). |
| 2026-08-31 | /implement TASK-07.1 concluído: Shell Next.js com sidebar (lista de projetos), topbar (usuário + logout), proteção de rotas (middleware), consumo de `/api/me` (obterMe), página inicial (lista projetos com links), login/logout via OIDC. Tokens visuais Design Brief 100% aplicados (paleta via variables, Google Fonts Inter, espaçamento 8px). |
| 2026-08-31 | /code-review TASK-07.1: APROVADO (3/3 critérios de aceite atendidos; 3 importantes identificados e corrigidos: I1=estrutura HTML válida para cards, I2=dropdown fecha ao clicar fora + ao navegar, I3=remove inline styles usa classes CSS; +S1 tratamento erro logout). 0 críticos, 0 importantes residuais, 0 findings segurança. |
| 2026-08-31 | /implement TASK-07.2 concluído: Board UI (TL-03 — cards compactos) com etapas×raias×cards; criar/excluir/mover cards; indicador visual de impedimento; cliente STOMP com reconexão + resincronização por seq. Route handlers criados: GET /api/board/{projetoId}, POST/DELETE /tarefas, POST /mover, POST/DELETE /impedimento. Componentes React: BoardLayout, Card, CreateCardModal. Integração completa com backend. |
| 2026-08-31 | /code-review TASK-07.2 (contexto fresco): APROVADO COM RESSALVAS (0 críticos, 2 importantes, 3 sugestões). Corrigidos durante review: C1 (autenticação STOMP) + C2 (race condition). Adiáveis: I1 (projetoFinalizado no backend), S1-S3 (visual improvements + testes). Safeguards confirmados: STOMP auth obrigatória, resincronização com lock. |
| 2026-08-31 | /implement TASK-07.3 iniciado: Detalhe da tarefa com lead-time/etapa (RF-006), auditoria (RF-017), edição com congelamento pós-início (RF-003), observadores (RF-005). Fase 1 concluída: tipos (LeadTimeEtapa, AuditoriaEntry, TarefaDetalhe) + API client (lib/api/tarefa.ts). Fase 2 em andamento: componentes LeadTimePanel, AuditoriaPanel, EditarTarefaForm, ObservadoresPanel. |
| 2026-08-31 | /implement TASK-07.3 frontend concluído: Página `tarefas/[tarefaId]/page.tsx` (orquestração + estado), 4 componentes funcionais, route handlers proxies (GET/PUT /tarefas/{id}, GET /auditoria, POST/DELETE /observadores/{usuarioId}). Navegação integrada no Card (Link ao título leva ao detalhe). Pendência: backend deve expor `projetoFinalizado` em GET /board (I1 de TASK-07.2, adiável), lista de usuários disponíveis para adicionar como observadores (TODO comentado em ObservadoresPanel). |
| 2026-08-31 | /code-review TASK-07.3: ❌ REPROVADO inicialmente (1 crítico, 2 importantes, 3 sugestões). C1: assinatura Next.js 14+ incompatível; I1: RF-005 incompleto; I2: validação URL ausente. |
| 2026-08-31 | Correções aplicadas (1 hora): C1 resolvido em 8 route handlers (tarefas + board); I1 implementado (novo endpoint `/api/projetos/{id}/usuarios`, carregamento em frontend, passa para ObservadoresPanel); I2 adicionada validação em proxy.ts. Build ✅ bem-sucedido. |
| 2026-08-31 | /code-review TASK-07.3 (pós-correções): ✅ APROVADO COM RESSALVAS (0 críticos residuais, 0 importantes residuais, 3 sugestões adiáveis). Código pronto para merge. Sugestões (S1-S3) são melhorias futuras. |
| 2026-09-01 | TASK-07.4 e TASK-07.5 commitadas (Admin UI projeto/workflows/raias/papéis/permissões/usuários) — `727ce45`, `fbdbc40`. |
| 2026-09-01 | /implement TASK-07.7 (Notificações UI) concluído — **EPIC 07 concluído**: `lib/types.ts` (tipo `Notificacao`), `lib/api/notificacoes.ts` (listar não lidas + marcar como lida), route handlers `app/api/notificacoes/route.ts` + `.../[id]/marcar-como-lida/route.ts`, `lib/notificacoes-stomp.ts` (cliente STOMP dedicado p/ `/topic/notificacoes/{usuarioId}`, token no CONNECT, reconexão backoff; payload é gatilho → recarrega lista via REST), `components/notificacoes/NotificacoesSino.tsx` (sino topbar, badge, painel, marcar-lida otimista), `DashboardShell.tsx` (placeholder → `<NotificacoesSino>`). Divergência: backend real é `GET /api/notificacoes` (sem query) + `PUT /{id}/marcar-como-lida` + campo `tarefaTitulo` — contrato techspec desatualizado; seguido o backend. `tsc` ✅ `vitest` ✅(5) `next build` ✅. E2E runtime pendente (sem Docker nesta sessão). |
| 2026-09-01 | /code-review TASK-07.7 (contexto fresco, agent general-purpose): ❌ REPROVADO — C1 (tempo real inoperante: clientes STOMP liam token de `document.cookie` `session=`, mas cookie é `kanban_session` httpOnly/cifrado), I1 (backend só `.withSockJS()`, incompatível com WS cru), I2 (dedup framing STOMP), I3 (contrato `dashboard-notificacoes.md` desatualizado). Defeitos C1/I1 herdados do board (TASK-07.2). |
| 2026-09-01 | Correção C1/I1 (opção "ticket de curta duração", cross-cutting board+notificações): **Backend** — `WsTicketService` (ticket HMAC-SHA256 stateless, TTL 30s), `WsTicketController` (`POST /api/ws-ticket`), `WsTicketAuthenticationFilter` (valida `?ticket=` em `/ws**`, popula Principal), `StompConfig` sem SockJS, `application.yml` `kanban.ws-ticket.secret`, `WsTicketServiceTest` (5 ✅). **Frontend** — `lib/api/proxy.ts` corrigido (delega p/ `lib/api.ts` → conserta auth de TODOS os route handlers que usavam `@/lib/api/proxy`), `app/api/ws-ticket/route.ts` + `lib/api/ws-ticket.ts`, `lib/stomp.ts` + `lib/notificacoes-stomp.ts` agora recebem `getTicket` e conectam em `/ws?ticket=`, `board/page.tsx` + `NotificacoesSino.tsx` sem hack de cookie. I3 reconciliado. `mvn -o test-compile` ✅, `tsc`/`vitest`/`next build` ✅. **Handshake WS ponta a ponta não exercitado em runtime** (sem Docker). I2 aceito como ressalva pós-merge. Canvas S +4 safeguards. |
| 2026-09-01 | /code-review TASK-07.7 (re-review pós-correção): **APROVADO COM RESSALVAS** (0 críticos, 0 importantes residuais, 4 sugestões pós-merge). Corrigidos no re-review: `StompManager` flag `encerrado` (reconexão pós-unmount + leak de `getTicket`); `lib/api.ts` `apiProxyFetch` seta `Content-Type: application/json` (senão 415 em todos os route handlers de mutação). **Ressalva bloqueante:** validar handshake WS ponta a ponta em runtime antes de fechar RF-005/EPIC 07. Relatório: `docs/checklists/kanban-tarefas-TASK-07.7-review.md`. |
| 2026-09-01 | /implement TASK-08.1 concluído: `MultiPodBroadcastIntegrationTest` (`backend/.../multipod/`) — pod A = `@SpringBootTest(RANDOM_PORT, "it")`, pod B = 2º `ConfigurableApplicationContext` via `SpringApplicationBuilder` no **mesmo Postgres** `kanban_it` (datasource/keycloak/ws-ticket repassados como args `--k=v`, precedência acima do `application-dev.yml`; sem Testcontainers). Casos: (1) evento de board do pod B → cliente STOMP do pod A < 2s, `@RepeatedTest(10)` (gate "0 falhas em 10 execuções"); (2) notificação do pod B → cliente do pod A em `/topic/notificacoes/{id}`; (3) SUBSCRIBE em pods distintos recebem o mesmo evento único < 2s. Resync client-side por gap de `seq` → `frontend/stomp.test.ts` (3 casos). `mvn -P integration-tests test -Dtest=MultiPodBroadcastIntegrationTest` → 12/12; `vitest` → 8/8; suíte completa `-P integration-tests` sem regressão (174 + repetições). |
| 2026-09-01 | **Ressalva bloqueante da TASK-07.7 fechada — smoke do handshake WS ponta a ponta em runtime.** Novo IT `WsHandshakeIntegrationTest` (`@SpringBootTest(RANDOM_PORT)` + `@ActiveProfiles("it")`, cliente STOMP nativo `StandardWebSocketClient`): exercita a cadeia real ticket→`/ws?ticket=`→`WsTicketAuthenticationFilter`→`SecurityFilterChain`→`BoardChannelInterceptor`→NOTIFY (`ListenNotifyPublisher`)→SimpleBroker→cliente. 3 casos verdes contra o stack Docker: (1) ticket válido + vínculo RBAC → evento entregue <2s (RNF-001); (2) ticket inválido → handshake 401; (3) ticket válido sem vínculo → SUBSCRIBE bloqueado silenciosamente (RNF-003 fail-closed). `mvn -P integration-tests test -Dtest=WsHandshakeIntegrationTest` → 3/3. `WS_TICKET_SECRET` já presente no `docker-compose.yml` (linha 52). **EPIC 07 MVP fechado.** |
| 2026-09-01 | TASK-07.7 — S2 + I1-resid resolvidos: `WsTicketService` sem default público, boot falha se `kanban.ws-ticket.secret` ausente/branco/<16 chars; secret movido para `application-{dev,test,it}.yml` (`application.yml` = `${WS_TICKET_SECRET:}`); `SecurityConfig` sem `/ws/info` permitAll + javadocs atualizados; `StompConfig` javadoc sem SockJS. `WsTicketServiceTest` 6 ✅. Nota: `mvn -o test` sem Docker = 40 erros pré-existentes (ITs `@ActiveProfiles("it")` sem Postgres). |
| 2026-09-01 | /code-review TASK-07.6: ✅ APROVADO (0 críticos, 0 importantes, 3 sugestões adiáveis: S1 import via barrel `@/lib/api`, S2 validação de UUID no route handler, S3 testes de `formatarTempo`/mapeamento de status). Gate: `tsc` ✅, `vitest` ✅ (5, sem testes de dashboard), eslint inutilizável (config v9 ausente no repo). Canvas S +2 safeguards (tipos TS espelham contrato 1:1 em segundos; dashboard sem gate de permissão client-side). Relatório: `docs/checklists/kanban-tarefas-TASK-07.6-review.md`. |
| 2026-09-01 | /implement TASK-07.6 (Dashboard UI) concluído: página `projetos/[id]/dashboard/page.tsx`, `components/dashboard/DashboardView.tsx` (tabela lead-time + impedimento médios por etapa, formatação de duração, total de tarefas consideradas), route handler proxy `GET /api/dashboard/[projetoId]` → `/api/projetos/{id}/dashboard`. Tipos `EtapaLeadTime`/`Dashboard` em `lib/types.ts` **realinhados ao contrato real do backend** (`leadTimeMedioPorEtapa`, `leadTimeMedioSegundos`, `tempoImpedimentoMedioSegundos`, `totalTarefasConsideradas`) — o rascunho anterior assumia campos inexistentes (agregado ms, `impedimentoPorEtapa`). Link de nav já existia em `DashboardShell`. `tsc --noEmit` ✅. Falta /code-review. |

## kanban-tarefas

- **Tasks Concluídas:**
  - `TASK-01.1` — Setup de projeto + Docker Compose + Keycloak dev
  - `TASK-01.2` — Migrations V1-V2 (Usuario, Projeto, Papel, Permissao)
  - `TASK-02.1` — OIDC + JIT Provisioning + /api/me + Logout
  - `TASK-02.2` — Motor RBAC + PermissaoGuard
  - `TASK-03.1` — CRUD de Projeto (incl. finalizar/reabrir)
  - `TASK-03.2` — CRUD Workflow/Etapa/Transicao (TDD + Review Aprovado)
  - `TASK-03.3` — CRUD Raia (TDD + Review Aprovado)
  - `TASK-04.1` — Migrations V5-V6 + entidade Tarefa + criação de card + fechamento real de RN-005
  - `TASK-04.2` — Mover tarefa: engine de transição + congelamento + lead-time + RN-012 (TDD)
  - `TASK-04.3` — Impedimento: marcar/desmarcar + histórico (TDD)
  - `TASK-04.4` — Exclusão de tarefa + auditoria (TDD + Review Inline Aprovado)
  - `TASK-04.5` — GET board + GET detalhe com projeção DTO (sem N+1, Review Aprovado, Testes Completos)
  - `TASK-05.1` — EventoBoardPublisher + LISTEN/NOTIFY + STOMP (Review Aprovado com ressalvas, 23 testes)
  - `TASK-05.2` — Notificações internas: Notificacao (entidade + V7), Service, Publisher (LISTEN/NOTIFY), Controller, integração com TarefaService (2026-08-29)
  - `TASK-05.3` — Resiliência LISTEN/NOTIFY: reconexão infinita + backoff, health readiness, métricas Micrometer (2026-08-29)
  - `TASK-06.1` — Dashboard: `GET /api/projetos/{id}/dashboard` — lead-time médio/etapa + impedimento/etapa por overlap (sem migration), RN-015 (Review Aprovado com ressalvas, 8 testes unitários) (2026-08-31) — **EPIC 06 concluído**
  - `TASK-07.1` — Shell Next.js + autenticação (sidebar, topbar, proteção rotas, `/api/me`, login/logout OIDC, Design Brief 100%) (2026-08-31) — **Review APROVADO**
  - `TASK-07.2` — Board UI (cards, mover, impedimento, STOMP realtime) (2026-08-31) — **Review APROVADO COM RESSALVAS**
  - `TASK-07.3` — Detalhe da tarefa (lead-time, auditoria, edição, observadores) (2026-08-31) — **Review APROVADO COM RESSALVAS**
  - `TASK-07.4` — Admin UI projeto/workflows/raias (2026-09-01, commit `727ce45`)
  - `TASK-07.5` — Admin UI papéis/permissões/usuários (2026-09-01, commit `fbdbc40`)
  - `TASK-07.6` — Dashboard UI (lead-time + impedimento médios/etapa) (2026-09-01) — **Review APROVADO**
  - `TASK-07.7` — Notificações UI (sino topbar, STOMP `/topic/notificacoes/{id}`, marcar como lida) + autenticação WS por ticket de curta duração (board+notificações) (2026-09-01) — **EPIC 07 concluído**
  - `TASK-08.1` — Testes multi-pod e WebSocket: `MultiPodBroadcastIntegrationTest` (2 contextos Spring / mesmo Postgres) + resync `seq` no frontend (2026-09-01) — 12/12 IT, 8/8 vitest
  - `TASK-08.3` — Dockerização de backend e frontend
- **Última Etapa:** TASK-08.1 concluída — `MultiPodBroadcastIntegrationTest` 12/12 verde (`@RepeatedTest(10)` de propagação board pod B→pod A + notificação multi-pod + 2 conexões em pods distintos), `frontend/stomp.test.ts` 3 casos de resync por gap de `seq` (2026-09-01)
- **Code review:** TASK-07.7 — APROVADO COM RESSALVAS — 2026-09-01
- **Findings:** 0 críticos, 0 importantes residuais, 4 sugestões pós-merge
- **Code review:** TASK-07.6 — APROVADO — 2026-09-01
- **Findings:** 0 críticos, 0 importantes, 3 sugestões
- **Testes:** suíte `-P integration-tests` 174 + `MultiPodBroadcastIntegrationTest` (12 = 10 repetições + 2) + `WsHandshakeIntegrationTest` 3 + `WsTicketServiceTest` 6 (unit). Frontend: `vitest` 8 (+3 de resync `seq` em `stomp.test.ts`). Execução ITs: `systems/CRUDAO/run-integration-tests.ps1`.
- **API Status:** ✅ Backend (imagem final) sobe no compose — Flyway V1-V7 validado, STOMP operacional. Actuator expõe `health,info,metrics`; grupo `readiness` reflete estado dos listeners LISTEN/NOTIFY.
- **Próximo passo recomendado:** TASK-08.2 (Observabilidade final — logback rotação, métricas Actuator, runbook stub Keycloak). Depois merge de `feature/quarta_vez`. Pendências abertas de PO (não bloqueiam EPIC 08): RN-002 "total agregado" no payload do dashboard (TASK-06.1); `projetoFinalizado` no GET /board (I1 de TASK-07.2/07.3). Ressalva pós-merge herdada: I2 (dedup framing STOMP) da TASK-07.7.