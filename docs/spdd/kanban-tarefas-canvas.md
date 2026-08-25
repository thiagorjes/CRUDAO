# REASONS Canvas — Kanban de Tarefas
_Status: DRAFT | Idioma: pt_BR | Iniciado em: 2026-08-24_

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

_Atualizado por: /tasks v1.0 — [pendente]_
> Decisões: —

**Tasks ordenadas por dependência:**
- [ ] TASK-01.1 — {{DESCRICAO_TASK}}

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

_Atualizado por: /code-review v1.0 — [pendente]_
> Decisões: —

**Restrições:**
- {{RESTRICAO_1}}

**O que NÃO fazer:**
- {{NAO_FAZER_1}}
