# TASK-02.1 — CRUD de Tarefa e movimentação entre etapas [G]

**Status:** Concluída — 2026-08-22 (code review agent QA: aprovado com ressalvas, findings corrigidos)

**Epic:** EPIC-02 — Tarefas, Board e Tempo Real | **User Story:** US-02.1 — Gestão de tarefas e movimentação no board
**Sistema:** CRUDAO | **RF:** RF-003, RF-002, RF-004, RF-012 | **Dependências:** TASK-01.2

---

## Contexto

Núcleo funcional do board: criar/editar/excluir tarefas, movê-las respeitando o workflow, sinalizar impedimento e suportar reabertura de tarefas finalizadas.

## O que deve ser feito

- [x] Implementar entidade Tarefa (com tipo, responsável, etapa/raia atual — enum TipoTarefa: FEATURE, BUG, CHORE)
- [x] CRUD de Tarefa (RF-003)
- [x] Endpoint `PATCH /api/tarefas/{id}/mover`, validando a transição contra o workflow (RF-002); impedimento não bloqueia nem libera movimentação — regra é só o workflow (RF-004 clarificado)
- [x] Suporte à transição `REABERTURA` para "desfinalizar" (RF-012)
- [x] Endpoint de marcar/desmarcar impedimento (`POST`/`DELETE /api/tarefas/{id}/impedimento`)
- [x] Suporte a mover tarefa entre projetos (`PATCH /api/tarefas/{id}/mover-projeto`) — enforcement de permissão (admin com acesso em ambos) fica para TASK-04.1 (RBAC ainda não existe), marcado com TODO no código

## Guia técnico

- Pacote: `domain/tarefa`
- Referência: `docs/techspec/kanban-configuravel-techspec.md` seção 4 (contratos REST)

## Critérios de aceite

- Movimentação de tarefa só ocorre se a transição for permitida pelo workflow (teste unitário)
- Marcar/desmarcar impedimento funciona independentemente da posição no workflow
- Cenários Gherkin do PRD (RF-002, RF-004, RF-012) cobertos a 100% (BDD)

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
