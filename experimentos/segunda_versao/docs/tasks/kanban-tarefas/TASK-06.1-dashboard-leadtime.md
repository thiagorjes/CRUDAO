# TASK-06.1 — Migration V7 + agregação de lead-time médio

**Status:** Concluída — 2026-08-26

**Tamanho:** [G] 1-2 dias
**Sistema:** CRUDAO
**RF de origem:** RF-007
**Dependências:** TASK-04.5
**Paralelismo:** nenhum (pode iniciar em paralelo à Epic 05 assim que TASK-04.5 concluída)

## Contexto

Visibilidade para gestores sem necessidade de acompanhar a execução diretamente — segundo objetivo central do PRD.

## O que deve ser feito

- [x] ~~Criar migration V7 (Notificacao)~~ — já criada em TASK-05.2; nome desatualizado no texto original desta task. Migration nova desta task: **V11** (adiciona `etapa_id` a `tarefa_impedimento_historico`).
- [x] Implementar `GET /api/projetos/{projetoId}/dashboard`: lead-time médio por etapa + tempo médio de impedimento agregado (RN-001, RN-002), a partir de `TarefaEtapaHistorico`/`TarefaImpedimentoHistorico`.
- [x] Garantir acessibilidade do dashboard mesmo com projeto finalizado (RN-015 — leitura permitida, sem `exigirProjetoAtivo`).

## Guia técnico

- `backend/src/main/resources/db/migration/V7__notificacao.sql`
- `backend/src/main/java/.../dashboard/DashboardService.java`
- Contrato: `docs/techspec/kanban-tarefas/contracts/dashboard-notificacoes.md`.

## Critérios de aceite

- Dashboard agrega lead-time médio corretamente com histórico de múltiplas tarefas/etapas (dataset controlado de teste).
- Tempo médio de impedimento agregado calculado corretamente.
- Acessível com projeto finalizado.
