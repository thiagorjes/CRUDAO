# TASK-05.2 — Frontend: Dashboard de gestão [M]

**Epic:** EPIC-05 — Frontend Next.js | **User Story:** US-05.1 — Interfaces do sistema
**Sistema:** CRUDAO | **RF:** RF-007 | **Dependências:** TASK-03.1

---

## Contexto

Implementar a tela de dashboard conforme o design brief e o protótipo aprovado (seletor de período, gráfico de barras, tabela).

## O que deve ser feito

- [ ] Implementar seletor de período (data início/fim) fixo no topo
- [ ] Disparar job assíncrono e tratar loading com skeleton screen (DDR-003)
- [ ] Renderizar gráfico de barras + tabela de lead-time médio por etapa e tempo médio em impedimento
- [ ] Aplicar fallback de polling caso WebSocket não esteja disponível

## Guia técnico

- Referência: `docs/design/kanban-configuravel-design-brief.md`
- Protótipo aprovado: `docs/design/prototypes/kanban-configuravel/Dashboard.dc.html` (fonte) — Artifact: https://claude.ai/code/artifact/a7612319-88d0-434d-9729-64d3d1604c6c

## Critérios de aceite

- Dashboard reflete o design aprovado no protótipo
- Skeleton exibido durante o processamento assíncrono, substituído pelo resultado ao concluir

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
