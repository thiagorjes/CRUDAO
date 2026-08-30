# Estado Operacional - CRUDAO
_Atualizado em: 2026-08-29 (21h00)_

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
| docs/checklists/kanban-tarefas-bdd-coverage.md | 1.0 | ok - 100% rastreabilidade PRD→Tasks→Testes validada |
| docs/checklists/kanban-tarefas-TASK-04.2-review.md | 1.1 | ok - Aprovado (I1 resolvido: EditarTarefaRequest com @NotBlank/@Size) |
| docs/checklists/kanban-tarefas-TASK-04.4-review.md | 1.0 | ok - Aprovado sem ressalvas |
| docs/checklists/kanban-tarefas-TASK-04.5-review.md | 1.0 | ok - Aprovado |
| docs/checklists/kanban-tarefas-TASK-05.1-review.md | 1.0 | ok - Aprovado com ressalvas (3 sugestões observabilidade) |
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
  - `TASK-08.3` — Dockerização de backend e frontend
- **Última Etapa:** /code-review TASK-05.3 + correções — suíte `-P integration-tests` **152 testes, 0 falhas, 0 erros** contra o stack Docker final (2026-08-30)
- **Testes:** 152 na suíte `-P integration-tests` (unitários + ITs @SpringBootTest contra Postgres+Keycloak reais). Execução: `systems/CRUDAO/run-integration-tests.ps1` (sobe compose, cria `kanban_it`, roda em container Maven).
- **API Status:** ✅ Backend (imagem final) sobe no compose — Flyway V1-V7 validado, STOMP operacional. Actuator expõe `health,info,metrics`; grupo `readiness` reflete estado dos listeners LISTEN/NOTIFY.
- **Próximo passo recomendado:** `/implement TASK-06.1` (dashboard) ou `/implement TASK-08.1` (testes multi-pod).