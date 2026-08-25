# TASK-07.3 — Detalhe da tarefa

**Tamanho:** [M] 4-8h
**Sistema:** CRUDAO
**RF de origem:** RF-003, RF-006, RF-017
**Dependências:** TASK-07.2, TASK-04.5
**Paralelismo:** nenhum

## Contexto

Visão detalhada por card — lead-time por etapa, histórico de auditoria, edição de campos.

## O que deve ser feito

- [ ] Exibir lead-time por etapa e tempo total de impedimento acumulado (RF-006).
- [ ] Exibir histórico de auditoria (RF-017).
- [ ] Formulário de edição respeitando congelamento pós-início (campos estruturais desabilitados quando `iniciada=true`).
- [ ] Gerenciar observadores explícitos (adicionar/remover).

## Guia técnico

- `frontend/app/projetos/[id]/tarefas/[tarefaId]/`

## Critérios de aceite

- Lead-time exibido bate com o retornado pelo backend, incluindo etapa em andamento.
- Campos estruturais aparecem desabilitados/bloqueados quando tarefa iniciada.
- Histórico de auditoria exibido em ordem cronológica.
