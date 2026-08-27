# TASK-05.2 — Notificações internas

**Status:** Concluída — 2026-08-25

**Tamanho:** [M] 4-8h
**Sistema:** CRUDAO
**RF de origem:** RF-005
**Dependências:** TASK-05.1, TASK-04.3
**Paralelismo:** nenhum

## Contexto

Endereça a motivação central do PRD — impedimento não visto a tempo (caso registrado de demanda parada por 5 dias).

## O que deve ser feito

- [ ] Ao mudar etapa (TASK-04.2) ou marcar/desmarcar impedimento (TASK-04.3), resolver lista de observadores (responsável + criador + `TarefaObservador`).
- [ ] Criar uma `Notificacao` por observador (tipos `TRANSICAO_ETAPA`, `IMPEDIMENTO_MARCADO`, `IMPEDIMENTO_DESMARCADO`).
- [ ] Publicar pelo canal dedicado `NOTIFY notificacao_events`; cada pod retransmite via STOMP para `/topic/notificacoes/{usuarioId}`, conforme a decisão revisada na TechSpec Seção 5. A autorização do `SUBSCRIBE` permanece no backend.
- [ ] Implementar `GET /api/notificacoes` (lista de não lidas) e endpoint de marcar como lida.
- [ ] Implementar CRUD de `TarefaObservador` (adicionar/remover observador explícito).

## Guia técnico

- `backend/src/main/java/.../notificacao/` — service, controller, repository.
- Contrato: `docs/techspec/kanban-tarefas/contracts/dashboard-notificacoes.md`.
- `docs/techspec/kanban-tarefas/data-model.md` — seções Notificacao, TarefaObservador.

## Critérios de aceite

- Alteração de etapa/impedimento gera `Notificacao` para responsável + criador + observadores explícitos.
- `GET /api/notificacoes` retorna apenas não lidas do usuário autenticado.
- Marcar como lida funciona e reflete em consultas subsequentes.
- Notificação chega ao cliente correto independentemente do pod que processou o evento (validação completa em TASK-08.1).
