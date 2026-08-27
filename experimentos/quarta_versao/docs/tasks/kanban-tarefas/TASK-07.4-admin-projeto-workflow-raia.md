# TASK-07.4 — Admin: projeto/workflow/etapa/transição/raia

**Tamanho:** [M] 4-8h
**Sistema:** CRUDAO
**RF de origem:** RF-008, RF-009, RF-010, RF-011
**Dependências:** TASK-07.1, TASK-03.1, TASK-03.2, TASK-03.3
**Paralelismo:** [P] com TASK-07.5, TASK-07.6, TASK-07.7

## Contexto

Configuração do board por projeto.

## O que deve ser feito

- [ ] Telas de CRUD de projeto (incl. finalizar/reabrir), workflow, etapa (com reordenação), transição, raia.
- [ ] Validação de UX espelhando RN-003/RN-005 (com revalidação sempre no backend).

## Guia técnico

- `frontend/app/projetos/[id]/admin/`

## Critérios de aceite

- Todas as operações de CRUD refletem corretamente as respostas de erro do backend (403, 422, bloqueio por tarefas ativas).

## Status: Concluída — 2026-08-26

_Marcação retroativa — implementação já registrada em `memory/state.md` (entrada "/implement TASK-07.4") e no commit `5f924df fix: findings TASK-07.4`; só o marcador estava ausente neste arquivo._
