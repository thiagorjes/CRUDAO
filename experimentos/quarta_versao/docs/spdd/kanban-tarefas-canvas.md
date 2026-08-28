# REASONS Canvas - Kanban de Tarefas
_Status: DRAFT | Idioma: pt_BR | Iniciado em: 2026-08-27_

> Canvas de rastreabilidade da feature. O status muda para READY somente quando R, E, A, S (Structure), O, N e S (Safeguards) estiverem preenchidas.

---

## R — Requirements

_Atualizado por: /spdd-canvas v1.0 - 2026-08-27_
> Decisoes: —

- Reduzir o tempo de execucao e de impedimento das atividades.
- Eliminar comunicacao dispersa sobre status e impedimentos.
- Dar visibilidade de andamento e lead-time aos gestores.

**RFs Must Have:** RF-001 a RF-013 e RF-015 a RF-019.  
**RF Should Have:** RF-014 (login via SSO/Keycloak).

**Escopo IN:** board com workflows, etapas, transicoes e raias configuraveis; CRUD e movimentacao de tarefas; impedimentos e notificacoes internas; lead-time por etapa e agregado; RBAC por projeto; SSO; auditoria.

**Escopo OUT:** notificacoes externas; timesheet; multiplas organizacoes/clientes; dependencias entre projetos; importacao em massa, templates, duplicacao e anexos.

---

## E — Entities

_Atualizado por: /spdd-canvas v1.0 - 2026-08-27_
> Decisoes: ADR-005, ADR-007

**Fonte de verdade:** [data-model.md](../techspec/kanban-tarefas/data-model.md).

- Identidade e acesso: `Usuario`, `Projeto`, `Papel`, `Permissao`, `PapelPermissao`, `UsuarioProjetoPapel`.
- Fluxo: `Workflow`, `Etapa`, `Transicao`, `Raia`.
- Execucao: `Tarefa`, `TarefaObservador`, `TarefaEtapaHistorico`, `TarefaImpedimentoHistorico`, `TarefaAuditoria`.
- Comunicacao: `Notificacao`.
- Schema versionado por migrations Flyway; `Usuario.adminGlobal` e criado pela migration V9.

---

## A — Approach

_Atualizado por: /spdd-canvas v1.0 - 2026-08-27_
> Decisoes: ADR-004, ADR-005, ADR-006, ADR-007, ADR-008

API REST e WebSocket/STOMP em Spring Boot, consumida por frontend Next.js. Keycloak fornece OIDC sem fallback local. PostgreSQL persiste o estado e tambem transporta eventos multi-pod por LISTEN/NOTIFY. Flyway e a fonte de verdade do schema. RBAC e modelado por papeis escopados por projeto, com bypass controlado para `adminGlobal`.

Trade-offs aceitos: LISTEN/NOTIFY nao oferece replay e limita payload a 8KB; a disponibilidade depende do Keycloak; imagens Docker mantem paridade de execucao entre ambientes.

---

## S — Structure

_Atualizado por: /spdd-canvas v1.0 - 2026-08-27_
> Decisoes: ADR-004, ADR-008

Backend em camadas `Controller -> Service -> Repository -> DTO/Mapper`, com MapStruct e Bean Validation. `TarefaService` concentra transicoes, congelamento, impedimentos e lead-time; guards validam RBAC no backend. Adapters de LISTEN/NOTIFY retransmitem eventos para topicos STOMP de board e notificacoes. Frontend Next.js possui shell, telas administrativas, board, dashboard e cliente realtime.

Dependencias externas: Keycloak/OIDC e PostgreSQL. Backend e frontend rodam como servicos Docker em `systems/CRUDAO/`.

---

## O — Operations

_Atualizado por: /tasks v1.0 - 2026-08-27_
> Decisoes: —

**Tasks ordenadas por dependência (26 tasks em 8 epics):**
- [ ] TASK-01.1 — Setup de projeto, Docker Compose e Keycloak dev
- [ ] TASK-01.2 — Migrations V1-V2 de identidade e RBAC
- [ ] TASK-02.1 — OIDC, provisioning JIT, `/api/me` e logout
- [ ] TASK-02.2 — Motor de permissões efetivas e guard (TDD)
- [ ] TASK-02.3 — CRUD de papéis, permissões e usuários
- [ ] TASK-03.1 — CRUD de projeto
- [ ] TASK-03.2 — CRUD de workflow, etapas e transições
- [ ] TASK-03.3 — CRUD de raias
- [ ] TASK-04.1 — Migrations de tarefas e criação de card
- [ ] TASK-04.2 — Movimentação, congelamento e lead-time
- [ ] TASK-04.3 — Marcação e desmarcação de impedimento
- [ ] TASK-04.4 — Exclusão e auditoria de tarefa
- [ ] TASK-04.5 — Board e detalhe em DTO
- [ ] TASK-05.1 — Eventos de board, LISTEN/NOTIFY e STOMP
- [ ] TASK-05.2 — Notificações internas
- [ ] TASK-05.3 — Reconexão, ressincronização e health-check
- [ ] TASK-06.1 — Dashboard de lead-time
- [ ] TASK-07.1 — Shell Next.js e autenticação
- [ ] TASK-07.2 — Board UI
- [ ] TASK-07.3 — Detalhe da tarefa UI
- [ ] TASK-07.4 — Administração de projeto/workflow/raia
- [ ] TASK-07.5 — Administração de papéis/permissões
- [ ] TASK-07.6 — Dashboard UI
- [ ] TASK-07.7 — Notificações UI
- [ ] TASK-08.1 — Testes multi-pod e WebSocket
- [ ] TASK-08.2 — Observabilidade final
- [ ] TASK-08.3 — Dockerização de backend e frontend

O detalhamento completo, o grafo de dependências e o backlog priorizado estão em [kanban-tarefas-tasks.md](../tasks/kanban-tarefas-tasks.md). Cada task possui também um arquivo autocontido em `docs/tasks/kanban-tarefas/`.

---

## N — Norms

_Atualizado por: /spdd-canvas v1.0 - 2026-08-27_
> Decisoes: ADR-005, ADR-006

- Validar toda permissao no backend, inclusive subscricoes STOMP; nunca confiar somente na UI.
- Usar Flyway para qualquer alteracao de schema e manter `ddl-auto=validate`.
- Seguir convencoes Java/TypeScript do sistema, Spotless/Checkstyle e ESLint/Prettier.
- Evitar nomes booleanos Java que gerem duas maiusculas apos o prefixo, como `eFinal`.
- Aplicar TDD para transicoes, lead-time e resolucao de permissoes; cobrir RF Must Have.
- Manter segredos e URLs dependentes de ambiente fora da imagem Docker.

---

## S — Safeguards

_Atualizado por: /code-review v1.0 - 2026-08-28 (TASK-04.3)_
> Decisoes: ADR-006, ADR-007, ADR-008

Guardrails consolidados a partir dos artefatos e confirmados como requisitos de revisão. A implementação da feature verificada task a task.

- Inventario normativo detalhado: [kanban-tarefas-safeguards.md](../checklists/kanban-tarefas-safeguards.md).
- O papel `admin` protegido nao pode ser editado, excluido, ter toggles alterados ou ser associado por um administrador local.
- Um usuario nao pode alterar permissoes do proprio papel; alteracoes geram auditoria.
- Projeto finalizado e somente leitura, inclusive para `adminGlobal`.
- Etapa não-final sem transição de saída configurada bloqueia salvamento/operacionalização (RN-003, HTTP 422 em `/api/etapas/{id}/transicoes`).
- Exclusão de Workflow/Etapa/Raia é bloqueada se houver tarefas ativas vinculadas (RN-005, HTTP 409).
- Raia default global (`projeto_id = NULL`) não pode ser editada ou excluída via chamadas de projeto (RN-CB-005, HTTP 403).
- Testes que sobem o contexto de seguranca exigem Keycloak e PostgreSQL disponiveis.
- O setup deve ser reproduzivel com Java 25 e runtime frontend alinhado a versao decidida na task; divergencias devem ser corrigidas ou documentadas.
- Credenciais presentes no realm sao somente de desenvolvimento e nao podem ser promovidas para producao.
- **Edição de tarefas (PUT /tarefas/{id}) deve validar entrada via Bean Validation em DTOs (tamanho máximo de titulo/descricao, nulidade) antes de persisted.**
- **Autorização de movimentação (POST /tarefas/{id}/mover) validada no backend (tarefa:finalizar se etapa final), nunca depende de UI.**
- **Lead-time calculado em segundos com precisão até Instant.now() para etapas em andamento; tempo de impedimento acumulado sobre múltiplos ciclos marca/desmarca.**
- **Marcação/desmarcação de impedimento (POST/DELETE /tarefas/{id}/impedimento) requer `tarefa:impedimento` no backend; histórico suporta múltiplos ciclos via marcadoEm/desmarcadoEm; auditoria grava cada alteração.**
- **Evitar duplicação de validação de guards (ex.: projeto finalizado já validado por permissaoGuard.exigirProjetoAtivo) — não replicar no método, confiar no contrato da guard.**

**Findings desta revisão:**

- TASK-03.2 e TASK-03.3 aprovadas sem ressalvas em 2026-08-28.
- TASK-04.2 aprovado com ressalvas (1 importante: validação de entrada em editar(), 2 sugestões menores) em 2026-08-28.
- TASK-04.3 aprovado com ressalvas (1 importante: verificação redundante de projeto finalizado removida, 1 sugestão: TODO para TASK-05.2) em 2026-08-28.

---

## Handoff

- **Proximo comando:** `/implement TASK-04.1` ou `/tdd TASK-04.1`
- **Review:** docs/checklists/kanban-tarefas-TASK-03.3-review.md — APROVADO
