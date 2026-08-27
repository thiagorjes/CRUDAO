# TASK-07.6 — Dashboard UI

**Tamanho:** [M] 4-8h
**Sistema:** CRUDAO
**RF de origem:** RF-007
**Dependências:** TASK-07.1, TASK-06.1
**Paralelismo:** [P] com TASK-07.4, TASK-07.5, TASK-07.7

## Contexto

Visão de gestão sem necessidade de acesso de execução.

## O que deve ser feito

- [x] Renderizar lead-time médio por etapa e tempo médio de impedimento agregado a partir de `GET /dashboard`.
- [x] Garantir acesso mesmo para papel `gestor` (sem `tarefa:gerenciar`/execução).

## Guia técnico

- `frontend/app/projetos/[id]/dashboard/`

## Critérios de aceite

- Dashboard acessível a gestor sem permissões de execução.
- Dados batem com o retornado pelo backend em dataset de teste.

## Status: Concluída — 2026-08-26
