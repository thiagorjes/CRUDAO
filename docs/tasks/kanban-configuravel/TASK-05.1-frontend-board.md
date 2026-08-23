# TASK-05.1 — Frontend: Board principal (drag-and-drop, cards, raias) [G]

**Epic:** EPIC-05 — Frontend Next.js | **User Story:** US-05.1 — Interfaces do sistema
**Sistema:** CRUDAO | **RF:** RF-001, RF-002, RF-004, RF-005, RF-012 | **Dependências:** TASK-02.2
**Status: Concluída — 2026-08-23**

---

## Contexto

Implementar a tela principal do sistema conforme o design brief e o protótipo aprovado (board com drag-and-drop e destaque de colunas válidas).

## O que deve ser feito

- [x] Implementar layout de board (colunas × raias) consumindo a API REST
- [x] Conectar ao WebSocket/STOMP para atualização em tempo real (<2s)
- [x] Implementar drag-and-drop real com destaque visual das colunas válidas (DDR-002)
- [x] Implementar menu do card (avançar/retroceder/desfinalizar)
- [x] Implementar indicador de impedimento (semáforo vermelho) e página de detalhe da tarefa
- [x] Aplicar tokens do design brief (cores, tipografia Roboto, espaçamento base 8px, desktop-only ≥1024px)

## Guia técnico

- Referência: `docs/design/kanban-configuravel-design-brief.md`
- Protótipo aprovado: `docs/design/prototypes/kanban-configuravel/Main.dc.html` (fonte) — Artifact: https://claude.ai/code/artifact/a7612319-88d0-434d-9729-64d3d1604c6c

## Critérios de aceite

- Board reflete o design aprovado no protótipo
- Drag-and-drop só permite drop em colunas com transição válida
- Atualizações de outros usuários aparecem em até 2s

## Nota técnica

Escopo consciente: RF-003 (criação de tarefa) não está no RF desta task — sem UI de criação de tarefa. Painel de Administração (TASK-05.3) e Dashboard (TASK-05.2) ficam fora daqui.

Dois endpoints de leitura adicionados ao backend, necessários para o board funcionar conforme os critérios de aceite: `GET /api/transicoes?workflowId=` (não existia — o frontend precisa da lista de transições para calcular destinos válidos do drag) e `GET /api/usuarios` (somente leitura — resolve `responsavelId` em nome/inicial no card).

## Validação

Via `docker compose` real: login → board carrega dados reais pelo proxy autenticado; `PATCH /tarefas/{id}/mover` para etapa sem transição válida retorna 409 (validação server-side); marcar/desmarcar impedimento reflete em `registros-etapa`; evento STOMP em `/topic/projetos/{id}/board` chega ao subscriber em ~200ms (RNF-001 exige <2s), testado com cliente STOMP real (`@stomp/stompjs`) contra o backend real. 43 testes unitários (vitest) verdes, lint e `next build` limpos; backend `mvn verify` verde.

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
