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

_Atualizado por: /spdd-canvas v1.0 - 2026-08-27_
> Decisoes: —

Pendente de preenchimento pela skill `/tasks`. O documento de tasks deve registrar dependencias, paralelismo, backlog priorizado e um arquivo autocontido por task. Enquanto O nao for preenchida, o canvas permanece DRAFT.

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

_Atualizado por: /spdd-canvas v1.0 - 2026-08-27_
> Decisoes: ADR-006, ADR-007

Pendente de validacao e preenchimento formal pela skill `/code-review`. Ate la, devem ser preservadas estas restricoes conhecidas:

- O papel `admin` protegido nao pode ser editado, excluido, ter toggles alterados ou ser associado por um administrador local.
- Um usuario nao pode alterar permissoes do proprio papel; alteracoes geram auditoria.
- Projeto finalizado e somente leitura, inclusive para `adminGlobal`.
- Testes que sobem o contexto de seguranca exigem Keycloak e PostgreSQL disponiveis.

---

## Handoff

- **Proximo comando:** `/tasks kanban-tarefas`
- **Transicao esperada:** `/tasks` preenche O e muda para READY somente apos as demais dimensoes serem confirmadas.
