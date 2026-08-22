# TASK-03.1 — Cálculo de lead-time, impedimento e Dashboard assíncrono [G]

**Epic:** EPIC-03 — Lead-time e Dashboard | **User Story:** US-03.1 — Métricas de andamento para gestão
**Sistema:** CRUDAO | **RF:** RF-006, RF-007 | **Dependências:** TASK-02.2

---

## Contexto

Dar visibilidade de lead-time por etapa e tempo em impedimento, com dashboard agregado por período configurável, calculado de forma assíncrona para não travar a UI ([ADR-005](../../decisions/ADR-005-dashboard-assincrono.md)).

## O que deve ser feito

- [ ] Implementar entidades RegistroEtapa e Impedimento (histórico de permanência e tempo impedido por etapa, RN-001, RN-002)
- [ ] Ao mover uma tarefa, fechar o RegistroEtapa da etapa anterior e abrir um novo na etapa destino
- [ ] Exibir na tarefa: tempo por etapa + observação de tempo em impedimento durante aquela etapa (RF-006)
- [ ] Endpoint `POST /api/projetos/{id}/dashboard/calcular` disparando cálculo `@Async`, respondendo com `jobId` (202)
- [ ] Entrega do resultado via STOMP (`/topic/projetos/{id}/dashboard/{jobId}`) com fallback de polling (`GET .../jobs/{jobId}`)
- [ ] Cálculo de lead-time médio por etapa e tempo médio em impedimento, filtrado pelo período (data início/fim) selecionado

## Guia técnico

- Pacote: `domain/leadtime`, `dashboard/`
- Referência: [ADR-005](../../decisions/ADR-005-dashboard-assincrono.md), techspec seção 4 (Dashboard assíncrono)

## Critérios de aceite

- Lead-time e tempo de impedimento calculados corretamente mesmo com múltiplos períodos de impedimento intercalados (teste unitário)
- Dashboard não bloqueia a requisição HTTP inicial — resposta imediata com `jobId`, resultado entregue posteriormente

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
