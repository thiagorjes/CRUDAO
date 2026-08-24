# TASK-01.2 — Raias (swimlanes) por projeto e default globais [M]

**Status:** Concluída — 2026-08-22

**Epic:** EPIC-01 — Domínio: Projeto, Workflow, Etapas e Raias | **User Story:** US-01.1 — Estrutura configurável de workflow por projeto
**Sistema:** CRUDAO | **RF:** RF-011 | **Dependências:** TASK-01.1

---

## Contexto

Suportar múltiplos desenvolvedores no mesmo board via raias horizontais, específicas do projeto ou default globais.

## O que deve ser feito

- [x] Implementar entidade Raia (projeto_id nullable = raia default global)
- [x] CRUD de Raia, com bloqueio de exclusão se houver tarefas (RN-005)
- [x] Regra: projeto sem raias próprias usa raias default globais, editáveis/removíveis pelo admin do projeto (clarificado no PRD)

## Guia técnico

- Pacote: `domain/raia`
- Referência: PRD RF-011 (v1.1, clarificação)

## Critérios de aceite

- Board de um projeto sem raias próprias retorna as raias default globais
- Admin do projeto consegue mantê-las, editá-las ou removê-las

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
