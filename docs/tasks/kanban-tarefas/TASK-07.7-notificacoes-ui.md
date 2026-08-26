# TASK-07.7 — Notificações UI

**Tamanho:** [P] ≤4h
**Sistema:** CRUDAO
**RF de origem:** RF-005
**Dependências:** TASK-07.1, TASK-05.2
**Paralelismo:** [P] com TASK-07.4, TASK-07.5, TASK-07.6

## Contexto

Fecha o ciclo de visibilidade de impedimento — objetivo central do PRD.

## O que deve ser feito

- [x] Lista de notificações não lidas, conectada a `/topic/notificacoes/{usuarioId}` (com filtro client-side por `usuarioId`, conforme decisão da TechSpec).
- [x] Ação de marcar como lida.

## Guia técnico

- `frontend/components/notificacoes/`

## Critérios de aceite

- Notificação aparece em tempo real quando o usuário é observador de uma tarefa alterada.
- Marcar como lida reflete no backend e na UI.

## Status: Concluída — 2026-08-26
