# TASK-07.2 — Board: colunas, raias, cards, criar/excluir, mover

**Tamanho:** [G] 1-2 dias
**Sistema:** CRUDAO
**RF de origem:** RF-001, RF-002, RF-004, RF-011, RF-018, RF-019
**Dependências:** TASK-07.1, TASK-04.5, TASK-05.1
**Paralelismo:** nenhum

## Contexto

Tela central do sistema.

## O que deve ser feito

- [ ] Renderizar board a partir de `GET /board` (colunas na ordem configurada, tarefas agrupadas por raia).
- [ ] Implementar criação de card ("Novo card" na etapa de menor ordem) e exclusão de card.
- [ ] Implementar movimentação (drag-and-drop ou ação equivalente) chamando `POST /mover`, com feedback de erro quando transição bloqueada.
- [ ] Implementar indicador visual de impedimento e ação marcar/desmarcar.
- [ ] Conectar ao STOMP `/topic/board/{projetoId}` para atualização em tempo real; implementar resincronização por gap de `seq` (conforme TASK-05.3).

## Guia técnico

- `frontend/app/projetos/[id]/board/`
- Referência visual: `docs/design/kanban-tarefas/screen-map.md`.

## Critérios de aceite

- Board reflete estado do backend e se atualiza em tempo real sem refresh manual (<2s, RNF-001) quando outro usuário move/cria/exclui um card.
- Transição bloqueada exibe mensagem de erro clara.
- Reconexão de WebSocket dispara resincronização via `GET /board`.
