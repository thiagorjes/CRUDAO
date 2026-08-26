# TASK-04.3 — Impedimento: marcar/desmarcar + histórico

**Tamanho:** [M] 4-8h
**Sistema:** CRUDAO
**RF de origem:** RF-004
**Dependências:** TASK-04.1
**Paralelismo:** [P] com TASK-04.2, TASK-04.4

## Contexto

Sinalização de bloqueio — base do KPI de redução de tempo parado por impedimento não visto (motivação central do PRD).

## O que deve ser feito

- [ ] Implementar `POST/DELETE /api/tarefas/{id}/impedimento`: exige `tarefa:impedimento` (RN-013 — dev e product_owner por padrão, gestor não).
- [ ] Ao marcar: abrir `TarefaImpedimentoHistorico` (`marcadoEm=now`), setar `Tarefa.impedida=true`/`impedidaDesde`.
- [ ] Ao desmarcar: fechar `TarefaImpedimentoHistorico` (`desmarcadoEm=now`), setar `impedida=false`.
- [ ] Suportar múltiplos ciclos marca/desmarca acumulando corretamente (RN-002 — validado no cálculo de lead-time em TASK-04.5/TASK-06.1).
- [ ] Gravar `TarefaAuditoria` (campo `impedimento`).

## Guia técnico

- `backend/src/main/java/.../tarefa/ImpedimentoService.java` (ou método dedicado em `TarefaService`).
- Contrato: `docs/techspec/kanban-tarefas/contracts/tarefas.md` (seção impedimento).

## Critérios de aceite

- Usuário sem `tarefa:impedimento` → `403`.
- Marcar/desmarcar reflete corretamente em `impedida`/`impedidaDesde` e no histórico.
- Múltiplos ciclos acumulam tempo de impedimento corretamente.
- Auditoria registrada em cada marca/desmarca.

**Status:** Concluída
