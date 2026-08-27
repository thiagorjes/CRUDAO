# TASK-02.2 — Tempo real (WebSocket/STOMP), broadcast multi-pod e observadores [G]

**Status:** Concluída — 2026-08-22

**Epic:** EPIC-02 — Tarefas, Board e Tempo Real | **User Story:** US-02.1 — Gestão de tarefas e movimentação no board
**Sistema:** CRUDAO | **RF:** RF-005, RNF-001, RNF-002 | **Dependências:** TASK-02.1

---

## Contexto

Entregar atualizações em tempo real (<2s) a todos os usuários conectados, incluindo observadores da tarefa, com consistência entre múltiplos pods via PostgreSQL LISTEN/NOTIFY ([ADR-004](../../decisions/ADR-004-broadcast-multi-pod-listen-notify.md)).

## O que deve ser feito

- [ ] Configurar endpoint STOMP (`/topic/projetos/{id}/board`)
- [ ] Implementar listener `LISTEN/NOTIFY` do PostgreSQL por pod, retransmitindo eventos recebidos via STOMP local
- [ ] Publicar `NOTIFY` a cada mudança relevante (movimentação, impedimento, criação de tarefa)
- [ ] Implementar entidade Observador (usuários cadastrados vinculados à tarefa, RN-007) e notificação a eles nas transições (RF-005)
- [ ] Teste de integração simulando 2+ pods (ou 2 conexões WebSocket) validando entrega consistente

## Guia técnico

- Pacote: `realtime/`
- Referência: [ADR-004](../../decisions/ADR-004-broadcast-multi-pod-listen-notify.md), techspec seção 5 (Arquitetura e Fluxo)

## Critérios de aceite

- Evento originado em um pod chega a clientes conectados a outro pod em até 2s (RNF-001)
- Observadores da tarefa recebem notificação em toda transição de etapa

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
