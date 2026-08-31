# TASK-06.1 — Migration V7 + agregação de lead-time médio

**Status:** Concluída — 2026-08-31 (/implement + /code-review APROVADO COM RESSALVAS; ITs adiados para /tests)

**Tamanho:** [G] 1-2 dias
**Sistema:** CRUDAO
**RF de origem:** RF-007
**Dependências:** TASK-04.5
**Paralelismo:** nenhum (pode iniciar em paralelo à Epic 05 assim que TASK-04.5 concluída)

## Contexto

Visibilidade para gestores sem necessidade de acompanhar a execução diretamente — segundo objetivo central do PRD.

## O que deve ser feito

- [x] **Sem migration nova.** O texto original mencionava "V7 (Notificacao)" (criada em TASK-05.2) e um rascunho posterior citava "V11 (`etapa_id` em `tarefa_impedimento_historico`)" — ambos descartados. O tempo de impedimento por etapa é derivado em leitura (ver Decisão de implementação abaixo).
- [x] Implementar `GET /api/projetos/{projetoId}/dashboard`: lead-time médio por etapa + tempo médio de impedimento agregado (RN-001, RN-002), a partir de `TarefaEtapaHistorico`/`TarefaImpedimentoHistorico`.
- [x] Garantir acessibilidade do dashboard mesmo com projeto finalizado (RN-015 — leitura permitida, sem `exigirProjetoAtivo`).

## Decisão de implementação (2026-08-31)

`TarefaImpedimentoHistorico` não referencia etapa. Em vez de adicionar `etapa_id` via migration + backfill + escrita na engine de impedimento (`TarefaService`, lógica congelada/safeguarded), o tempo de impedimento por etapa é calculado em leitura, por **sobreposição (overlap)** de cada intervalo de impedimento da tarefa com a janela `[entradaEm, saidaEm]` do `TarefaEtapaHistorico`. Atende RN-002 sem alterar schema nem tocar a engine. Volume baixo — contrato dispensa materialização.

## Guia técnico

- `backend/src/main/java/.../dashboard/DashboardService.java`, `DashboardResponse.java`, `DashboardController.java`
- Contrato: `docs/techspec/kanban-tarefas/contracts/dashboard-notificacoes.md`.

## Critérios de aceite

- Dashboard agrega lead-time médio corretamente com histórico de múltiplas tarefas/etapas (dataset controlado de teste).
- Tempo médio de impedimento agregado calculado corretamente.
- Acessível com projeto finalizado.
