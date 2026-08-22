# TASK-05.1 — Frontend: Board principal (drag-and-drop, cards, raias) [G]

**Epic:** EPIC-05 — Frontend Next.js | **User Story:** US-05.1 — Interfaces do sistema
**Sistema:** CRUDAO | **RF:** RF-001, RF-002, RF-004, RF-005, RF-012 | **Dependências:** TASK-02.2

---

## Contexto

Implementar a tela principal do sistema conforme o design brief e o protótipo aprovado (board com drag-and-drop e destaque de colunas válidas).

## O que deve ser feito

- [ ] Implementar layout de board (colunas × raias) consumindo a API REST
- [ ] Conectar ao WebSocket/STOMP para atualização em tempo real (<2s)
- [ ] Implementar drag-and-drop real com destaque visual das colunas válidas (DDR-002)
- [ ] Implementar menu do card (avançar/retroceder/desfinalizar)
- [ ] Implementar indicador de impedimento (semáforo vermelho) e página de detalhe da tarefa
- [ ] Aplicar tokens do design brief (cores, tipografia Roboto, espaçamento base 8px, desktop-only ≥1024px)

## Guia técnico

- Referência: `docs/design/kanban-configuravel-design-brief.md`
- Protótipo aprovado: `docs/design/prototypes/kanban-configuravel/Main.dc.html` (fonte) — Artifact: https://claude.ai/code/artifact/a7612319-88d0-434d-9729-64d3d1604c6c

## Critérios de aceite

- Board reflete o design aprovado no protótipo
- Drag-and-drop só permite drop em colunas com transição válida
- Atualizações de outros usuários aparecem em até 2s

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
