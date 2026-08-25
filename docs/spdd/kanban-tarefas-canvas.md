# REASONS Canvas — Kanban de Tarefas
_Status: READY | Idioma: pt_BR | Iniciado em: 2026-08-24_

> **Como usar:** cada skill do pipeline preenche suas dimensões ao concluir.
> Canvas transita de DRAFT → READY quando todas as 7 dimensões estiverem preenchidas.
> READY = canvas executável por outro agente de implementação.

---

## R — Requirements

_Atualizado por: /prd v1.0 — 2026-08-24_
> Decisões: —

- Reduzir tempo de execução e de impedimento das atividades
- Eliminar comunicação dispersa sobre status/impedimentos
- Dar visibilidade de andamento e lead-time aos gestores

**RFs Must Have:** RF-001, RF-002, RF-003, RF-004, RF-005, RF-006, RF-007, RF-008, RF-009, RF-010, RF-011, RF-012, RF-013, RF-015, RF-016, RF-017, RF-018, RF-019
**RFs Should Have:** RF-014 (Login via SSO/Keycloak)

**Escopo IN:**
- Board kanban com etapas e raias configuráveis por projeto, com workflows e transições configuráveis
- Criação/exclusão de card pelo board, atualização de status pelo próprio desenvolvedor
- Sinalização de impedimentos com notificação interna
- Lead-time visível por etapa (no board) e agregado (dashboard)
- Controle de acesso por papéis configuráveis escopados por projeto, com permissões via toggle e SSO (Keycloak)
- Histórico de auditoria da tarefa

**Escopo OUT:**
- Integração com sistemas externos de notificação (email, Slack etc.)
- Controle de horas/timesheet do desenvolvedor
- Suporte a múltiplas organizações/clientes
- Dependência entre projetos
- Importação em massa, templates, duplicação de card e anexos/arquivos

---

## E — Entities

_Atualizado por: /prd v1.0 — [pendente] / /designer v1.0 — 2026-08-25 / /techspec v1.0 — 2026-08-25_
> Decisões: —

**Entidades do data model (fonte de verdade: [data-model.md](../techspec/kanban-tarefas/data-model.md)):**
- Usuario, Projeto, Papel (protegido: `admin`), Permissao, PapelPermissao, UsuarioProjetoPapel
- Workflow, Etapa (com `etapaFinal`), Transicao, Raia (com raia default global)
- Tarefa, TarefaObservador, TarefaEtapaHistorico, TarefaImpedimentoHistorico, TarefaAuditoria, Notificacao

**Entidades de UX/UI (designer v1.0):**
- Telas: TL-01 Login, TL-02 Lista de Projetos, TL-03/03b Board (2 variações de densidade de card), TL-04 Detalhe da Tarefa, TL-05 Nova Tarefa, TL-06 Confirmação de Exclusão, TL-07 Dashboard, TL-08 Admin de Projeto, TL-09 Admin de Papéis, TL-10 Lista de Usuários
- Layout: Sidebar (navegação global) + Topbar (usuário/notificações)
- Design tokens: paleta Bootstrap-like (primary `#0d6efd`, secondary/success `#198754`, error `#dc3545`, warning `#ffc107`), fonte Inter, grid base 8px, tema Light only
- Referência: [docs/design/kanban-tarefas-design-brief.md](../design/kanban-tarefas-design-brief.md)

---

## A — Approach

_Atualizado por: /techspec v1.0 — 2026-08-25_
> Decisões: ADR-004, ADR-005, ADR-006

**Estratégia de solução:**
API REST + WebSocket/STOMP em Spring Boot, consumida por frontend Next.js. Autenticação via Keycloak (OIDC), sem fallback local. Broadcast de eventos entre pods via PostgreSQL LISTEN/NOTIFY (sem broker dedicado). Schema versionado via Flyway. RBAC modelado como papéis por projeto com permissões via toggle.

**Trade-offs aceitos:**
- LISTEN/NOTIFY: sem infra adicional, mas payload limitado a 8KB e sem replay garantido de eventos perdidos (ADR-004).
- Sem fallback de auth local: menor superfície de ataque, mas disponibilidade do sistema acoplada ao Keycloak (ADR-006).

---

## S — Structure

_Atualizado por: /techspec v1.0 — 2026-08-25_
> Decisões: ADR-004

**Arquitetura:**
Backend em camadas (Controller → Service → Repository → DTO/Mapper via MapStruct). Componentes principais: `TarefaService` (transições, congelamento, impedimento), `PermissaoService` (RBAC/toggles), listener LISTEN/NOTIFY + publisher STOMP para eventos de board/notificações. Frontend Next.js consumindo REST + STOMP.

**Dependências externas:**
- Keycloak (OIDC) — autenticação, sem fallback (ADR-006)
- PostgreSQL — persistência e broadcast de eventos (ADR-004)

---

## O — Operations

_Atualizado por: /tasks v1.0 — 2026-08-25_
> Decisões: —

**Tasks ordenadas por dependência (25 tasks em 8 epics — revisado pelo Comitê de Análise — ver [kanban-tarefas-tasks.md](../tasks/kanban-tarefas-tasks.md)):**
- [ ] TASK-01.1 — Setup projeto backend/frontend + docker-compose + Keycloak dev
- [ ] TASK-01.2 — Migrations V1-V2: Usuario/Projeto/Papel/Permissao
- [ ] TASK-02.1 — OIDC Keycloak + provisioning JIT + /api/me + logout
- [ ] TASK-02.2 — RBAC: motor de permissões efetivas + guard (TDD obrigatório)
- [ ] TASK-02.3 — CRUD papéis/permissões/usuários (dona da migration V8)
- [x] TASK-03.1 — CRUD Projeto incl. finalizar/reabrir
- [x] TASK-03.2 — CRUD Workflow/Etapa/Transicao
- [x] TASK-03.3 — CRUD Raia
- [x] TASK-04.1 — Migrations V5-V6 + criar card
- [ ] TASK-04.2 — Mover tarefa: transição + congelamento + lead-time
- [ ] TASK-04.3 — Impedimento: marcar/desmarcar
- [ ] TASK-04.4 — Excluir tarefa + Auditoria PapelPermissao
- [ ] TASK-04.5 — GET board + GET detalhe (projeção DTO)
- [ ] TASK-05.1 — EventoBoardPublisher + LISTEN/NOTIFY + STOMP
- [ ] TASK-05.2 — Notificações internas
- [ ] TASK-05.3 — Resiliência: reconexão, resync, health-check
- [ ] TASK-06.1 — Migration V7 + dashboard lead-time
- [ ] TASK-07.1 — Shell Next.js + auth
- [ ] TASK-07.2 — Board: colunas, raias, cards, mover
- [ ] TASK-07.3 — Detalhe da tarefa
- [ ] TASK-07.4 — Admin projeto/workflow/raia
- [ ] TASK-07.5 — Admin papéis/permissões
- [ ] TASK-07.6 — Dashboard UI
- [ ] TASK-07.7 — Notificações UI
- [ ] TASK-08.1 — Testes multi-pod / WebSocket
- [ ] TASK-08.2 — Observabilidade final

---

## N — Norms

_Atualizado por: /techspec v1.0 — 2026-08-25_
> Decisões: —

**Padrões relevantes para esta feature:**
- Schema só via Flyway — `ddl-auto=validate`, nunca `update`/`create` (ADR-005)
- Toda permissão validada no backend, nunca só na UI (RNF-003, `security.md`)
- Evitar boolean com duas maiúsculas seguidas após prefixo (ex.: usar `etapaFinal`, não `eFinal` — `coding-standards.md`)
- TDD obrigatório para engine de transições, cálculo de lead-time e resolução de permissões (`skill-conventions.md`)

---

## S — Safeguards

_Atualizado por: /implement TASK-02.3 — 2026-08-25_
> Decisões: —

**Restrições:**
- RN-006 — papel `admin` (global, `protegido=true`) nunca pode ser editado, excluído, ter seus toggles alterados, **nem associado a um usuário via `POST /api/projetos/{projetoId}/usuarios`** (achado de code review TASK-02.3 — escalação de privilégio: qualquer usuário com `papel:administrar` local conseguia se autoconceder o `admin` global antes da correção).
- RN-017 — usuário não pode alterar `PapelPermissao` de nenhum papel que ele próprio possui no projeto; toda alteração de toggle gera `PapelPermissaoAuditoria`.
- Testes de integração (`@SpringBootTest`/`@WebMvcTest` que sobem `SecurityConfig` real) exigem Keycloak acessível — `docker compose up -d keycloak postgres` antes de `mvn test` (ver `guidelines/testing.md` e `techspec/kanban-tarefas/quickstart.md`).

**O que NÃO fazer:**
- Não tratar chave de papel duplicada no projeto como caso "não vai acontecer" — a constraint `uk_papel_projeto_chave` estoura `DataIntegrityViolationException`; validar antes do `save` e responder `409`.
- Não confiar em `papel.getProjeto() != null` sozinho para validar se um papel pode ser associado — `admin` tem `projeto=null` e é o caso que mais precisa ser bloqueado (checar `protegido` primeiro).

---

_Atualizado por: /implement TASK-03.1 — 2026-08-25_

- **ADR-007** — bootstrap do primeiro admin via `Usuario.adminGlobal` (flag, não papel) setado no primeiro login do e-mail configurado em `kanban.bootstrap.admin-email`; `PermissaoGuard` bypassa RBAC escopado para `adminGlobal=true`, exceto `exigirProjetoAtivo` (RN-015 vale sempre, sem exceção para nenhum papel).
- **Guard reutilizável `PermissaoGuard.exigirProjetoAtivo(projetoId)`** (RN-015) — chamar em todo endpoint de escrita das epics 04+ antes de gravar; `KANBAN_BOOTSTRAP_ADMIN_EMAIL` é obrigatória em produção (sem ela, ninguém cria o primeiro projeto).

_Atualizado por: /implement TASK-03.2 — 2026-08-25_

- Não confiar em `findById(destinoId)` sozinho para validar destino de uma `Transicao` — sempre checar `destino.getWorkflow().getId().equals(origem.getWorkflow().getId())`; achado de code review (agent QA) — sem essa checagem, um admin de projeto conseguia criar transição apontando para etapa de workflow/projeto alheio.
- `DataIntegrityViolationException` de constraint `UNIQUE` (ex.: `uk_etapa_workflow_ordem`, `uk_transicao_origem_destino`) deve sempre virar `409`, nunca vazar como `500` — padrão recorrente entre tasks (já corrigido também em TASK-02.3).

_Atualizado por: /implement TASK-04.1 — 2026-08-25_

- RN-005 real (não mais stub): "tarefa ativa" = `etapaAtual.etapaFinal=false`. Checagem via `TarefaRepository.existsBy{Workflow,EtapaAtual,Raia}IdAndEtapaAtualEtapaFinalFalse`, usada por `WorkflowService`/`RaiaService` na exclusão (409 se houver tarefa ativa vinculada).
- **Um workflow por projeto passa a ser regra de serviço** (`WorkflowService.criar` bloqueia com `409` se o projeto já tiver workflow) — decisão fechada em code review (agent QA) para resolver a ambiguidade de "workflow ativo do projeto" citada em `contracts/tarefas.md`, já que o data-model não modela essa unicidade nem um flag de ativo. `TarefaService.criar` depende dessa garantia para escolher o workflow do projeto sem ambiguidade.
- `TarefaService.resolverResponsavel` não valida vínculo do usuário com o projeto na criação (qualquer `usuarioId` existente no sistema pode virar responsável) — gap de integridade registrado em code review, não bloqueante para esta task; revisar junto de RN-012 em TASK-04.2.
