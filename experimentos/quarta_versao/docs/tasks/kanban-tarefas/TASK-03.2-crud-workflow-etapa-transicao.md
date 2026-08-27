# TASK-03.2 — CRUD Workflow/Etapa/Transicao

**Status:** Concluída — 2026-08-25

**Tamanho:** [G] 1-2 dias
**Sistema:** CRUDAO
**RF de origem:** RF-002, RF-009, RF-010
**Dependências:** TASK-02.2
**Paralelismo:** [P] com TASK-03.1, TASK-03.3

## Contexto

Motor de workflow configurável — base para toda movimentação de tarefas (Epic 04).

## O que deve ser feito

- [ ] Criar migration V3 (Workflow, Etapa, Transicao).
- [ ] Implementar CRUD de Workflow, Etapa (reordenação incluída), Transicao.
- [ ] Validar RN-003 (etapa não-final exige ≥1 transição de saída) em nível de serviço na criação/edição de Etapa.
- [ ] Preparar RN-005 (bloquear exclusão de workflow/etapa com tarefas ativas vinculadas): como a entidade `Tarefa` só existe a partir de TASK-04.1, implementar aqui apenas um stub que sempre retorna "sem tarefas ativas" (decisão fechada pelo Comitê de Análise — sem ambiguidade). **A checagem real é implementada obrigatoriamente em TASK-04.1**, que substitui este stub — não é opcional nem uma alternativa em aberto.

## Guia técnico

- `backend/src/main/resources/db/migration/V3__workflow_etapa_transicao.sql`
- `backend/src/main/java/.../workflow/` — controller, service, repository.
- Contrato: `docs/techspec/kanban-tarefas/contracts/workflows.md`.
- `docs/techspec/kanban-tarefas/data-model.md` — seções Workflow, Etapa, Transicao.

## Critérios de aceite

- Etapa não-final sem transição de saída configurada → erro `422` ao tentar salvar/operacionalizar.
- Reordenação de etapas persiste corretamente (`ordem`).
- `UNIQUE(etapaOrigemId, etapaDestinoId)` respeitado em Transicao.
- Stub de RN-005 responde "sem tarefas ativas" (implementação real fica em TASK-04.1, fora do escopo de aceite desta task).
