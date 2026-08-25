# TASK-03.3 — CRUD Raia (swimlanes)

**Tamanho:** [M] 4-8h
**Sistema:** CRUDAO
**RF de origem:** RF-011
**Dependências:** TASK-02.2
**Paralelismo:** [P] com TASK-03.1, TASK-03.2

## Contexto

Agrupamento visual de tarefas no board — inclui raia default global usada quando o card é criado sem raia (RN-CB-005).

## O que deve ser feito

- [ ] Criar migration V4 (Raia, incl. seed de raia default global com `projetoId=null`).
- [ ] Implementar CRUD de Raia.
- [ ] Preparar RN-005 (bloquear exclusão com tarefas ativas vinculadas): implementar aqui apenas um stub que sempre retorna "sem tarefas ativas" (mesma decisão fechada de TASK-03.2). **A checagem real é implementada obrigatoriamente em TASK-04.1.**

## Guia técnico

- `backend/src/main/resources/db/migration/V4__raia.sql`
- `backend/src/main/java/.../raia/` — controller, service, repository.
- Contrato: `docs/techspec/kanban-tarefas/contracts/raias.md`.
- `docs/techspec/kanban-tarefas/data-model.md` — seção Raia.

## Critérios de aceite

- Raia default global existe após seed e é usada quando projeto não tem raia própria.
- Tarefas agrupadas visualmente pelas raias configuradas (validação de contrato; UI real em TASK-07.2).
- Stub de RN-005 responde "sem tarefas ativas" (implementação real fica em TASK-04.1, fora do escopo de aceite desta task).
