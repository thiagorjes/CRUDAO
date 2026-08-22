# TASK-02.1 — CRUD de Tarefa e movimentação entre etapas [G]

**Epic:** EPIC-02 — Tarefas, Board e Tempo Real | **User Story:** US-02.1 — Gestão de tarefas e movimentação no board
**Sistema:** CRUDAO | **RF:** RF-003, RF-002, RF-004, RF-012 | **Dependências:** TASK-01.2

---

## Contexto

Núcleo funcional do board: criar/editar/excluir tarefas, movê-las respeitando o workflow, sinalizar impedimento e suportar reabertura de tarefas finalizadas.

## O que deve ser feito

- [ ] Implementar entidade Tarefa (com tipo, responsável, etapa/raia atual — ver enum TipoTarefa a definir, Q-004 da techspec)
- [ ] CRUD de Tarefa (RF-003)
- [ ] Endpoint `PATCH /api/tarefas/{id}/mover`, validando a transição contra o workflow (RF-002); impedimento não bloqueia nem libera movimentação — regra é só o workflow (RF-004 clarificado)
- [ ] Suporte à transição `REABERTURA` para "desfinalizar" (RF-012)
- [ ] Endpoint de marcar/desmarcar impedimento (`POST`/`DELETE /api/tarefas/{id}/impedimento`)
- [ ] Suporte a mover tarefa entre projetos por admin com permissão em ambos (confirmado na entrevista de techspec)

## Guia técnico

- Pacote: `domain/tarefa`
- Referência: `docs/techspec/kanban-configuravel-techspec.md` seção 4 (contratos REST)

## Critérios de aceite

- Movimentação de tarefa só ocorre se a transição for permitida pelo workflow (teste unitário)
- Marcar/desmarcar impedimento funciona independentemente da posição no workflow
- Cenários Gherkin do PRD (RF-002, RF-004, RF-012) cobertos a 100% (BDD)

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
