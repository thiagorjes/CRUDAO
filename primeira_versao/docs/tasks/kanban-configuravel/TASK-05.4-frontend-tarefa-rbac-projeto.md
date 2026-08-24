# TASK-05.4 — Frontend: ajustes de tarefa para RBAC por projeto [M]

**Epic:** EPIC-05 — Frontend Next.js | **User Story:** US-05.1 — Interfaces do sistema
**Sistema:** CRUDAO | **RF:** RF-003, RF-012, RF-017 | **Dependências:** TASK-02.3, TASK-05.1

---

## Contexto

Ajustes incrementais na página de detalhe da tarefa e no board (já implementados na TASK-05.1), para expor os comportamentos novos do PRD v1.2: histórico de auditoria, autoatribuição, e a permissão dedicada de finalizar/desfinalizar.

## O que deve ser feito

- [x] Página de detalhe da tarefa (`/tarefas/[id]`): seção de histórico (`GET /historico`) listando as alterações (quem, o quê, de/para, quando)
- [x] Botão "Atribuir a mim" (self-assign/"puxar") sempre visível a qualquer membro do projeto; campo de reatribuir a outro usuário só habilitado se `GET /usuarios/me` indicar `tarefa:atribuir` no projeto (gating de UX — backend revalida)
- [x] Trava de edição de título/descrição: campo somente-leitura quando a tarefa já está "iniciada" e o usuário não tem `devPodeEditarTarefaIniciada` habilitado nem papel que dispense a trava (backend é a fonte real — 403 tratado como erro se o usuário tentar mesmo assim)
- [x] Menu do card / ação "avançar para etapa final" e "desfinalizar" (já existente na TASK-05.1) — se a chamada retornar 403 por falta de `tarefa:finalizar`, exibir modal de erro claro em vez de erro genérico

## Guia técnico

- Referência: `frontend/src/app/tarefas/[id]/page.tsx`, `frontend/src/components/board/` (já implementados na TASK-05.1), `docs/techspec/kanban-configuravel-techspec.md` (v1.2, contrato de `/historico`, `/responsavel`)

## Critérios de aceite

- Histórico da tarefa exibe as alterações registradas pelo backend (TASK-02.3), em ordem cronológica reversa
- "Atribuir a mim" funciona mesmo quando a tarefa já está atribuída a outro dev
- Tentativa de mover para etapa final sem permissão exibe erro claro, não falha silenciosa

---

**Status:** Concluída — 2026-08-24

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
