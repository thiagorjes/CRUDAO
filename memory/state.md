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
| docs/spdd/kanban-tarefas-canvas.md | — | READY (7/7 dimensões preenchidas — S/Safeguards preenchida em TASK-02.3) |

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

- **Etapa concluída:** /implement TASK-03.1 — 2026-08-25
- **Task implementada:** TASK-03.1 — CRUD de Projeto incl. finalizar/reabrir (RF-008, RN-015) — 2026-08-25
- **Arquivos:** systems/CRUDAO/backend/src/main/resources/db/migration/V9__usuario_admin_global.sql (novo); .../domain/usuario/Usuario.java (+`adminGlobal`); .../auth/UsuarioProvisioningService.java (+bootstrap por `kanban.bootstrap.admin-email`); .../rbac/PermissaoGuard.java (+bypass admin global, +`exigirProjetoAtivo`); .../projeto/{ProjetoController,ProjetoService,CriarProjetoRequest,EditarProjetoRequest,ProjetoResponse}.java (novo pacote); application.yml + application-dev.yml (+property `kanban.bootstrap.admin-email`); testes .../projeto/ProjetoServiceTest.java (novo), .../rbac/PermissaoGuardTest.java e PermissaoGuardEndpointIT.java (+admin global/projeto ativo), .../auth/UsuarioProvisioningServiceTest.java (+bootstrap)
- **Ambiguidade resolvida com o usuário:** `POST /api/projetos` exige `projeto:administrar`, mas essa permissão é escopada por `UsuarioProjetoPapel` (projeto_id NOT NULL) — sem projeto existente, ninguém consegue ser vinculado ao papel `admin` global para criar o primeiro projeto. Decisão do usuário: seed de um usuário admin global real (cadastrado no Keycloak, já existia `admin.teste` no realm dev de TASK-01.1), que configura os demais depois do primeiro login. Documentado em **ADR-007** (novo) — `Usuario.adminGlobal` setado no primeiro login pelo `UsuarioProvisioningService` via property `kanban.bootstrap.admin-email` (não seed SQL — `keycloak_sub` só existe após login real). `PermissaoGuard` bypassa RBAC escopado para admin global, exceto `exigirProjetoAtivo` (RN-015 sem exceção para nenhum papel).
- **Guard reutilizável entregue:** `PermissaoGuard.exigirProjetoAtivo(projetoId)` (RN-015) — a ser chamado pelos endpoints de escrita das epics 04+ antes de gravar.
- **Testes:** `mvn test` executado — **50/50 verdes** (unitários + `PermissaoGuardEndpointIT`, `PapelPermissaoMigrationIT`, `PapelPermissaoAuditoriaMigrationIT` contra Keycloak real). Ambiente local sem JDK 25 (pom pinado nessa versão) — compilado/rodado com `-Dmaven.compiler.release=21` só para validação local; pom não foi alterado.
- **Próxima task:** TASK-03.2 / TASK-03.3 (paralelas) — CRUD Workflow/Etapa/Transicao / Raia

_Etapa anterior:_

- **Etapa concluída:** /implement TASK-02.3 — 2026-08-25
- **Task implementada:** TASK-02.3 — CRUD de papéis/permissões/usuários (RN-006, RN-017), dona da migration V8 — 2026-08-25
- **Arquivos:** systems/CRUDAO/backend/src/main/resources/db/migration/V8__papel_permissao_auditoria.sql (novo); .../domain/papel/{PapelPermissaoAuditoria,PapelPermissaoAuditoriaRepository}.java (novo); .../domain/papel/{PapelRepository,UsuarioProjetoPapelRepository}.java (+métodos); .../rbac/{PermissaoService,PermissaoGuard}.java (+`possuiVinculo`/`membro`, para GETs que exigem só vínculo ao projeto — não `papel:administrar`); .../papel/{PapelService,UsuarioProjetoPapelService,PapelController,UsuarioProjetoController}.java + DTOs (novo pacote); testes .../papel/{PapelServiceTest,UsuarioProjetoPapelServiceTest}.java e .../domain/papel/PapelPermissaoAuditoriaMigrationIT.java
- **Decisão de arquitetura:** autorização de `PUT/DELETE /api/papeis/{id}` e do toggle é resolvida dentro de `PapelService` (não via `@PreAuthorize` no controller) porque só o service, após carregar o `Papel`, sabe a que projeto ele pertence; usa `permissaoGuard.exigir(...)` imperativamente. RN-017 checa se o autor autenticado possui o papel-alvo entre seus vínculos no projeto antes de permitir o toggle.
- **Testes:** 3 arquivos novos — unitários (Mockito) para `PapelService` (chave `admin` reservada, chave duplicada no projeto, papel protegido bloqueado sem chamar guard, RN-017 negado/permitido, auditoria registrada) e `UsuarioProjetoPapelService` (associação bem-sucedida e bloqueio de papel protegido/global); IT de migration V8 (Testcontainers) validando persistência de `PapelPermissaoAuditoria`.
- **`mvn test` executado de fato pela primeira vez neste sandbox** (achado + destravado nesta task): o `settings.xml` global do Maven local forçava mirror para o Nexus corporativo inacessível — contornado com `-gs` apontando para settings sem mirror (Maven Central). Resultado: **25/25 testes unitários passam** (incl. os 9 novos da TASK-02.3); **`PapelPermissaoMigrationIT` e `PapelPermissaoAuditoriaMigrationIT` (V1/V2/V8) passam 4/4 + 1/1** contra Keycloak real (`docker compose up -d keycloak postgres`, realm `kanban-dev`) — valida a persistência de `PapelPermissaoAuditoria` fim a fim.
- **3 bugs pré-existentes descobertos e corrigidos** (fora do escopo direto da TASK-02.3, mas bloqueavam a build — corrigidos com autorização do usuário): (1) `RestClientOidcDiscoveryClient.java` (TASK-02.1) usava `ClientHttpRequestFactorySettings`/`Builder`, API do Spring Framework 7 inexistente no Boot 3.5.16 pinado — trocado por `SimpleClientHttpRequestFactory` (compatível); (2) `PermissaoGuardEndpointIT.java` (TASK-02.2) importava `@Import` do pacote errado (`test.context.annotation` em vez de `context.annotation`) — corrigido; (3) mesmo teste também precisava de `@MockBean UsuarioProvisioningService` (dependência de `AtivoUsuarioFilter`, incluído automaticamente na fatia `@WebMvcTest` por ser `Filter`) — adicionado.
- **Achado de infraestrutura de teste registrado em `guidelines/testing.md` e `techspec/kanban-tarefas/quickstart.md`:** `spring-boot-starter-oauth2-client` resolve o issuer OIDC eagerly na subida do `ApplicationContext` — todo teste que sobe `SecurityConfig` real (`@SpringBootTest`/`@WebMvcTest` com segurança real) exige Keycloak acessível.
- **4º bug pré-existente encontrado e corrigido (investigação do 404):** causa raiz não era conflito de rota — `EndpointDeTeste` (controller `@RestController` aninhado dentro da própria classe de teste) nunca virava bean; `@WebMvcTest(controllers = EndpointDeTeste.class)` sozinho **não registra** uma classe aninhada da própria classe de teste (o component-scan do slice não alcança para lá), então toda requisição caía no `ResourceHttpRequestHandler` (fallback estático) → 404 silencioso. Corrigido adicionando `EndpointDeTeste` também ao `@Import(...)` do teste (confirmado via diagnóstico: `RequestMappingHandlerMapping.getHandlerMethods()` só listava `/error` antes da correção, `GET /test/rbac/{projetoId}` depois). **Suíte completa validada: 33/33 testes verdes** (25 unitários + `PermissaoGuardEndpointIT` 3/3 + `PapelPermissaoMigrationIT` 4/4 + `PapelPermissaoAuditoriaMigrationIT` 1/1, todos contra Keycloak real).
- **Code review:** agent QA (contexto fresco) — 1 finding 🔴 corrigido: `UsuarioProjetoPapelService.associar` permitia conceder o papel `admin` global (protegido, RN-006) via CRUD de associação, escalação de privilégio — corrigido bloqueando papéis `protegido`/globais na associação (422); 1 finding 🟡 corrigido: violação de `uk_papel_projeto_chave` (chave duplicada no projeto) não tratada, retornaria 500 — corrigido com checagem prévia → 409. Findings 🟢 (método antes "morto" `findByProjetoIdAndChave`, e ausência de proteção contra "último admin se autorremover" — fora do escopo desta task, não é critério de aceite) não bloqueiam.
- **Canvas:** transitou para **READY** — dimensão S (Safeguards) preenchida nesta task.
- **Próxima task:** TASK-03.1 / TASK-03.2 / TASK-03.3 (paralelas) — CRUD Projeto / Workflow-Etapa-Transicao / Raia (opcional: investigar o 404 de `PermissaoGuardEndpointIT` antes)

_Etapa anterior:_

- **Task TDD:** TASK-02.2 — RBAC: motor de permissões efetivas + guard reutilizável — 2026-08-25
- **Arquivos:** systems/CRUDAO/backend/src/main/java/.../rbac/{PermissaoService,PermissaoGuard}.java (novo); .../security/SecurityConfig.java (+`@EnableMethodSecurity`); .../domain/papel/PapelPermissaoRepository.java (+`findByPapelIdIn` com JOIN FETCH); testes .../rbac/{PermissaoServiceTest,PermissaoGuardTest,PermissaoGuardEndpointIT}.java
- **Decisão de arquitetura:** `PermissaoService.permissoesEfetivas` revalida `Usuario.ativo` direto do banco a cada chamada (não confia no contexto de autenticação) — cobre desativação sem revogação de sessão (TechSpec RN). `PermissaoGuard` é bean `@Component("permissaoGuard")` usável tanto via `@PreAuthorize("@permissaoGuard.permitido(#projetoId, 'chave')")` (controllers das próximas epics — exige `@PathVariable("projetoId")` explícito, pom não compila com `-parameters`) quanto imperativamente via `guard.exigir(...)` (lança `AccessDeniedException` → 403 automático).
- **Testes:** 3 arquivos novos — unitários (Mockito) para `PermissaoService` (inativo, sem vínculo, toggle desabilitado, união de múltiplos papéis) e `PermissaoGuard` (sem usuário no contexto, permitido, negado); teste de endpoint (`@WebMvcTest` + `@EnableMethodSecurity` + `@PreAuthorize` real) confirmando 403/200 (RNF-003, "teste por endpoint"). Ciclo TDD Red→Green→Refactor seguido; execução real (`mvn test`) não realizada neste sandbox — mesma limitação de rede (Nexus `nexus3-cicd-tools.cloud.sfb` inacessível) das tasks anteriores. Validar localmente antes de considerar 100% fechada.
- **Code review:** agent QA (contexto fresco) — 3 findings 🟡, todos corrigidos: N+1 em `PermissaoService` (→ `findByPapelIdIn` com JOIN FETCH), chave de permissão vazando na mensagem de `AccessDeniedException` (→ mensagem genérica), teste de endpoint sem cenário "sem usuário no contexto" (→ adicionado).
- **Próxima task:** TASK-02.3 — CRUD papéis/permissões/usuários (dona da migration V8) — pode rodar em paralelo com TASK-03.1/03.2/03.3
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
