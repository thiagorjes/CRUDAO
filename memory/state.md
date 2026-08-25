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

- **Etapa concluída:** /implement TASK-04.3 — 2026-08-25
- **Task implementada:** TASK-04.3 — Impedimento: marcar/desmarcar + histórico (RF-004, RN-013) — 2026-08-25
- **Arquivos:** systems/CRUDAO/backend/src/main/java/com/crudao/kanban/tarefa/{TarefaService,TarefaController}.java (métodos/endpoints `marcarImpedimento`/`desmarcarImpedimento`, `POST`/`DELETE /api/tarefas/{id}/impedimento`); teste .../tarefa/TarefaServiceTest.java (+11 casos)
- **Decisões:** reusa entidades/repositório já criados em TASK-04.1 (`TarefaImpedimentoHistorico`); segue os mesmos padrões de TASK-04.2 (`exigirProjetoAtivoParaTarefa` 403→409, `tarefa:impedimento` via RBAC configurável — não hardcoded por papel). Múltiplos ciclos marca/desmarca suportados sem checagem extra além do estado `impedida` — RN-002 já é validada no cálculo de lead-time existente (`TarefaService.detalhe`/`tempoImpedimento`, TASK-04.2).
- **Testes:** `mvn test` (sem ITs) — **112/112 verdes** (36 em `TarefaServiceTest`, incl. 11 novos desta task, incl. caso dedicado de múltiplos ciclos acumulando tempo).
- **Code review:** agent QA (contexto fresco, persona via general-purpose) — 0 findings 🔴/🟡. 4 findings 🟢: 2 corrigidos (teste `desmarcarImpedimento_projetoFinalizado_lanca409` simétrico ao de marcar; teste dedicado de múltiplos ciclos acumulando tempo — ambos tocavam critério de aceite explícito da task; `valorAnterior` da auditoria de desmarcar trocado de literal fixo para o dado real do histórico). 2 não corrigidos (baixo risco, não bloqueantes): `TarefaResponse` de marcar/desmarcar não expõe `impedida`/`impedidaDesde` (cliente pode buscar via `GET /detalhe`).
- **Próxima task:** TASK-04.4 (excluir tarefa + auditoria) — paralela a TASK-04.2/TASK-04.3, depende só de TASK-04.1

_Etapa anterior:_

- **Etapa concluída:** /tdd TASK-04.2 — 2026-08-25
- **Task implementada (TDD):** TASK-04.2 — Mover tarefa: engine de transição + congelamento + lead-time (RF-002, RF-003, RF-006, RF-012, RN-004, RN-011, RN-012, RN-016) — 2026-08-25
- **Arquivos:** systems/CRUDAO/backend/src/main/java/com/crudao/kanban/tarefa/{TarefaService,TarefaController}.java (métodos `mover`/`editar`/`detalhe` novos, `criar` ajustado); .../tarefa/{MoverTarefaRequest,EditarTarefaRequest,HistoricoEtapaResponse,TarefaDetalheResponse}.java (novo); .../domain/tarefa/{TarefaEtapaHistoricoRepository,TarefaImpedimentoHistoricoRepository}.java (+métodos de consulta por tarefa); teste .../tarefa/TarefaServiceTest.java (+17 casos: mover, editar, detalhe)
- **Ciclo TDD:** Red → Green → Refactor → Review concluídos.
- **Endpoints novos:** `POST /api/tarefas/{id}/mover` (transição validada via `Transicao`, `tarefa:finalizar` se destino/origem é etapa final — cobre "desfinalizar" no mesmo endpoint), `PUT /api/tarefas/{id}` (congelamento de `titulo`/`descricaoEscopo` pós-início, RN-012 na troca de responsável), `GET /api/tarefas/{id}` (lead-time por etapa incl. etapa em andamento + tempo de impedimento total).
- **RN-012:** "gestão" (atribuição livre) mapeada para a permissão `tarefa:gerenciar` (não string de papel `dev`) — consistente com o RBAC 100% baseado em chaves de permissão do projeto.
- **Gap de TASK-04.1 fechado:** `resolverResponsavelVinculado` (usado por `criar` e `editar`) agora exige vínculo do usuário ao projeto via `UsuarioProjetoPapelRepository`, não só existência do usuário.
- **Testes:** `mvn test` (sem ITs) — **102/102 verdes** (26 em `TarefaServiceTest`, incl. 17 novos desta task).
- **Code review:** agent QA (contexto fresco, persona via general-purpose) — 0 findings 🔴; 2 findings 🟡 corrigidos: (1) projeto finalizado em `mover`/`editar` retornava `403` (guard genérico `PermissaoGuard.exigirProjetoAtivo`, reuso de RN-015) mas o contrato `tarefas.md` documenta `409` para esse caso — corrigido com wrapper local `exigirProjetoAtivoParaTarefa` que traduz para `409` sem alterar o guard compartilhado (mantém `criar`/demais epics em `403`, comportamento já testado); (2) `PUT /api/tarefas/{id}` não permitia desatribuir responsável (`responsavelId=null` indistinguível de "não enviado") — adicionado `EditarTarefaRequest.removerResponsavel` (exige `tarefa:gerenciar`). Findings 🟢 (concorrência em histórico de etapa aberto sem lock; falta de teste "gestor reatribui tarefa não iniciada") não corrigidos — baixo risco, não bloqueantes.
- **Próxima task:** TASK-04.3 (impedimento) ou TASK-04.4 (excluir tarefa + auditoria) — paralelas entre si e com TASK-04.2, ambas dependem só de TASK-04.1

_Etapa anterior:_

- **Etapa concluída:** /implement TASK-04.1 — 2026-08-25
- **Task implementada:** TASK-04.1 — Migrations V5-V6 + entidade Tarefa + criação de card pelo board (RF-018, RN-CB-001/003/004/005), dona da checagem real de RN-005 — 2026-08-25
- **Arquivos:** systems/CRUDAO/backend/src/main/resources/db/migration/{V5__tarefa,V6__tarefa_historico_auditoria}.sql (novo); .../domain/tarefa/{Tarefa,TarefaObservador,TarefaEtapaHistorico,TarefaImpedimentoHistorico,TarefaAuditoria,TarefaRepository,TarefaObservadorRepository,TarefaEtapaHistoricoRepository,TarefaImpedimentoHistoricoRepository,TarefaAuditoriaRepository}.java (novo pacote); .../tarefa/{TarefaService,TarefaController,CriarTarefaRequest,TarefaResponse}.java (novo pacote); .../domain/raia/RaiaRepository.java (+`findByProjetoIdOrderByOrdem`); .../workflow/WorkflowService.java (RN-005 real + regra "um workflow por projeto"); .../raia/RaiaService.java (RN-005 real); testes .../tarefa/TarefaServiceTest.java (novo), .../workflow/WorkflowServiceTest.java e .../raia/RaiaServiceTest.java (+casos RN-005/409)
- **RN-005 fechada de vez:** `TarefaRepository.existsBy{Workflow,EtapaAtual,Raia}IdAndEtapaAtualEtapaFinalFalse` — "ativa" = etapa atual não é etapa final. Substitui os stubs de TASK-03.2/TASK-03.3 em `WorkflowService`/`RaiaService` (409 na exclusão com tarefa ativa vinculada).
- **Decisão de code review (agent QA) aplicada:** `WorkflowService.criar` agora bloqueia com `409` a criação de um segundo workflow no mesmo projeto — resolve a ambiguidade de "workflow ativo do projeto" citada em `contracts/tarefas.md` sem alterar migration já aplicada (V3). `TarefaService.criar` depende dessa garantia para escolher o workflow do projeto sem ambiguidade (antes escolhia arbitrariamente o primeiro de uma lista sem `ORDER BY`).
- **Gap registrado, não bloqueante:** `TarefaService.resolverResponsavel` não valida vínculo do usuário responsável com o projeto na criação — revisar junto de RN-012 em TASK-04.2.
- **Testes:** `mvn test` (sem ITs) — **85/85 verdes** (9 novos em `TarefaServiceTest`, +2 em `WorkflowServiceTest`, +1 em `RaiaServiceTest`).
- **Code review:** agent QA (contexto fresco, persona via general-purpose) — 0 findings 🔴; 2 findings 🟡 corrigidos: workflow escolhido sem critério de "ativo" quando há múltiplos por projeto (→ regra de serviço "um workflow por projeto"), cobertura de teste incompleta (adicionados casos de responsável inexistente, workflow sem etapas, raia explícita válida do próprio projeto, conteúdo do `TarefaEtapaHistorico` salvo). Nitpicks 🟢 (ausência de `try/catch DataIntegrityViolationException` em `Tarefa.save` — sem `UNIQUE` relevante hoje, não bloqueante; falta de validação de vínculo do responsável ao projeto — registrado acima) não corrigidos nesta task.
- **Próxima task:** TASK-04.2 — Mover tarefa: transição + congelamento + lead-time (inclui RN-012, revisar `resolverResponsavel` junto)

_Etapa anterior:_

- **Etapa concluída:** /implement TASK-03.3 — 2026-08-25
- **Task implementada:** TASK-03.3 — CRUD de Raia (swimlanes), incl. migration V4 com seed de raia default global `projeto_id=null` (RF-011, RN-CB-005) — 2026-08-25
- **Arquivos:** systems/CRUDAO/backend/src/main/resources/db/migration/V4__raia.sql (novo); .../domain/raia/{Raia,RaiaRepository}.java (novo pacote); .../raia/{RaiaService,RaiaController,RaiaResponse,CriarRaiaRequest,EditarRaiaRequest}.java (novo pacote); teste .../raia/RaiaServiceTest.java (novo)
- **Padrão seguido:** idêntico a TASK-03.2 (Workflow) — leitura via `permissaoGuard.membro` (só vínculo ao projeto), escrita via `permissaoGuard.exigir("workflow:administrar")` + `exigirProjetoAtivo` (RN-015). Raia default global (`projeto=null`) nunca editável/excluível via `PUT/DELETE /api/raias/{id}` — bloqueada com `422` em `buscarRaiaDoProjeto`.
- **RN-005:** stub `possuiTarefasAtivasNaRaia` sempre `false`, mesma decisão fechada de TASK-03.2/TASK-04.1 (Comitê de Análise) — substituição obrigatória em TASK-04.1.
- **Testes:** `mvn test` (sem ITs) — suíte completa verde, incl. 13 casos novos em `RaiaServiceTest` (listar com/sem vínculo, fallback para raia global, criar/editar/excluir autorizado e não autorizado, nome vazio, ordem negativa, raia global bloqueada, 404).
- **Code review:** agent QA (contexto fresco, persona via general-purpose) — 0 findings 🔴; 2 findings 🟡 corrigidos: ausência de `UNIQUE(projeto_id, ordem)` na migration V4 (→ `uk_raia_projeto_ordem`, tratamento de `DataIntegrityViolationException` → `409` em `criar`/`editar`, consistente com `Etapa`), cobertura de teste incompleta (adicionados casos de nome vazio, sem-permissão em editar/excluir, projeto finalizado, exclusão de raia global). Nitpick 🟢 "buscar recurso antes de autorizar" (mesmo padrão já aceito em TASK-03.2) mantido por consistência.
- **Próxima task:** TASK-04.1 — Migrations V5-V6 + criar card (dona da checagem real de RN-005, obrigatória)

_Etapa anterior:_

- **Etapa concluída:** /implement TASK-03.2 — 2026-08-25
- **Task implementada:** TASK-03.2 — CRUD Workflow/Etapa/Transicao, dona da migration V3 (RF-002, RF-009, RF-010) — 2026-08-25
- **Arquivos:** systems/CRUDAO/backend/src/main/resources/db/migration/V3__workflow_etapa_transicao.sql (novo); .../domain/workflow/{Workflow,Etapa,Transicao,WorkflowRepository,EtapaRepository,TransicaoRepository}.java (novo pacote); .../workflow/{WorkflowService,WorkflowController,EtapaController} + DTOs (novo pacote); teste .../workflow/WorkflowServiceTest.java (novo)
- **RN-003:** validada em nível de serviço — editar etapa para não-final sem transição de saída existente, ou `PUT /api/etapas/{id}/transicoes` com lista vazia numa etapa não-final → `422` (contrato permite criar etapa sem transição; bloqueio só ao "operacionalizar").
- **RN-005:** stub isolado em `possuiTarefasAtivasNoWorkflow`/`possuiTarefasAtivasNaEtapa` (sempre `false`), Javadoc apontando substituição obrigatória em TASK-04.1 — decisão fechada pelo Comitê de Análise, não é opcional.
- **Testes:** `mvn test` (sem ITs) — **58/58 verdes** (16 no `WorkflowServiceTest`).
- **Code review:** agent QA (contexto fresco, persona via general-purpose) — 1 finding 🔴 corrigido: `PUT /api/etapas/{id}/transicoes` não validava que a etapa de destino pertencia ao mesmo workflow da origem (permitia transição cross-projeto/cross-workflow); 5 findings 🟡 corrigidos: `DataIntegrityViolationException` de `UNIQUE(workflowId,ordem)`/`UNIQUE(etapaOrigemId,etapaDestinoId)` virava 500 (→ 409), N+1 em `GET /workflows` (uma query de transições por etapa → 1 query agrupada), falta de bloqueio de transição etapa→ela mesma (→ 422), cobertura de teste incompleta (adicionados casos de conflito 409, cross-workflow e auto-loop), `ordem` negativa não validada (→ 422 na criação/edição). Finding 🟡 "404 antes de 403" (padrão já existente em ProjetoService/PapelService) mantido por consistência com o restante do projeto — não corrigido isoladamente aqui. Nitpick de `save` redundante em `editarEtapa` após reordenação corrigido (agora um único `saveAll`).
- **Próxima task:** TASK-03.3 (paralela, já concluída anteriormente confirmar) ou TASK-04.1

_Etapa anterior:_

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
