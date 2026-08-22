# REASONS Canvas — Kanban Configurável
_Status: DRAFT | Idioma: pt_BR | Iniciado em: 2026-08-22_

> **Como usar:** cada skill do pipeline preenche suas dimensões ao concluir.
> Canvas transita de DRAFT → READY quando todas as 7 dimensões estiverem preenchidas.
> READY = canvas executável por outro agente de implementação.

---

## R — Requirements

_Atualizado por: /prd v1.0 — 2026-08-22_
> Decisões: —

**Objetivos de negócio:**
- Reduzir tempo de execução e de impedimento das atividades
- Eliminar comunicação dispersa sobre status/impedimentos
- Dar visibilidade de andamento e lead-time aos gestores

**RFs Must Have:** RF-001 a RF-013 (board configurável, workflows/transições, CRUD de tarefas/projetos/workflows/colunas/raias, impedimento, notificação a observadores, lead-time por etapa e de impedimento, dashboard, etapa final com reabertura, controle de acesso por papéis configuráveis)
**RFs Should Have:** RF-014 (login SSO via Keycloak)

**Escopo IN:**
- Board kanban com colunas e raias configuráveis por projeto
- Workflows com transições configuráveis entre etapas
- Atualização de status e sinalização de impedimentos pelos devs
- Notificação (interna) a observadores em transições
- Lead-time por etapa e lead-time de impedimento, visível na tarefa e no dashboard
- Controle de acesso por papéis configuráveis (além de admin/user padrão)
- Visualização por gestores de outros times sem necessidade de atualização

**Escopo OUT:**
- Integração com sistemas externos de notificação (email, Slack etc.)
- Controle de horas/timesheet do desenvolvedor
- Suporte a múltiplas organizações/clientes
- Dependência entre projetos (bloqueio cruzado)

---

## E — Entities

_Atualizado por: /prd v1.0 — {{DATE}}_
> Decisões: —

{{DIAGRAMA_MERMAID_DOMINIO}}

**Entidades principais (rascunho — a refinar em /prd e /techspec):**
- Projeto: contexto que define o conjunto configurável de etapas do kanban
- Etapa (coluna): configurável por projeto, ordenada, usada para medir lead-time
- Tarefa/Atividade: unidade de trabalho movida entre etapas
- Impedimento: sinalização vinculada a uma tarefa, gera notificação
- Usuário: dev ou gestor, com participação registrada por tarefa

---

## A — Approach

_Atualizado por: /techspec v1.0 — {{DATE}}_
> Decisões: —

**Estratégia de solução:**
{{ESTRATEGIA_DE_SOLUCAO}}

**Trade-offs aceitos:**
- {{TRADEOFF_1}}

---

## S — Structure

_Atualizado por: /techspec v1.0 — {{DATE}}_
> Decisões: —

**Arquitetura:**
{{ARQUITETURA}}

**Dependências externas:**
- {{DEPENDENCIA_1}}

---

## O — Operations

_Atualizado por: /tasks v1.0 — {{DATE}}_
> Decisões: —

**Tasks ordenadas por dependência:**
- [ ] TASK-01.1 — {{DESCRICAO_TASK}}
- [ ] TASK-01.2 — {{DESCRICAO_TASK}}

---

## N — Norms

_Atualizado por: /guidelines v1.0 — {{DATE}}_
> Decisões: —

**Padrões relevantes para esta feature:**
- {{PADRAO_1}}
- {{PADRAO_2}}

---

## S — Safeguards

_Atualizado por: /code-review v1.0 — {{DATE}}_
> Decisões: —

**Restrições:**
- {{RESTRICAO_1}}

**O que NÃO fazer:**
- {{NAO_FAZER_1}}
