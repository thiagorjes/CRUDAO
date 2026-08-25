# TASK-04.1 — Migrations V5-V6 + entidade Tarefa + criação de card

**Status:** Concluída — 2026-08-25

**Tamanho:** [G] 1-2 dias
**Sistema:** CRUDAO
**RF de origem:** RF-018
**Dependências:** TASK-02.2, TASK-03.2, TASK-03.3
**Paralelismo:** nenhum

## Contexto

Núcleo do domínio — todas as demais tasks de Epic 04/05/06 dependem da entidade Tarefa existir.

## O que deve ser feito

- [ ] Criar migration V5 (Tarefa, TarefaObservador) e V6 (TarefaEtapaHistorico, TarefaImpedimentoHistorico, TarefaAuditoria), incluindo os índices de suporte a agregação: `(projetoId, etapaAtualId)`, `(responsavelId)` em Tarefa; `(tarefaId, entradaEm)` e `(etapaId, saidaEm)` em TarefaEtapaHistorico; `(tarefaId, marcadoEm)` em TarefaImpedimentoHistorico; `(tarefaId, dataHora)` em TarefaAuditoria.
- [ ] Implementar `POST /api/tarefas` (criar card pelo board): sem responsável se não informado (RN-CB-004), etapa de menor ordem + primeira raia do projeto ou raia default global (RN-CB-005), exige `tarefa:gerenciar` (RN-CB-001), bloqueado se projeto finalizado (RN-CB-003).
- [ ] Ao criar, abrir o primeiro `TarefaEtapaHistorico` (`entradaEm=now`, `saidaEm=null`).
- [ ] **Obrigatório:** implementar a checagem real de RN-005 (bloqueio de exclusão com tarefas ativas vinculadas) em `WorkflowService`/`EtapaService`/`RaiaService`, substituindo o stub deixado por TASK-03.2/TASK-03.3 (achado do Comitê de Análise — sem esta implementação, RN-005 nunca funciona de fato em produção).

## Guia técnico

- `backend/src/main/resources/db/migration/V5__tarefa.sql`, `V6__tarefa_historico_auditoria.sql`
- `backend/src/main/java/.../tarefa/` — entidade `Tarefa`, `TarefaObservador`, repositórios.
- Contrato: `docs/techspec/kanban-tarefas/contracts/tarefas.md` (seção POST).
- `docs/techspec/kanban-tarefas/data-model.md` — seções Tarefa, TarefaObservador, TarefaEtapaHistorico, TarefaImpedimentoHistorico, TarefaAuditoria.

## Critérios de aceite

- Criação sem responsável/raia usa defaults corretos (RN-CB-004, RN-CB-005).
- Sem `tarefa:gerenciar` → `403`.
- Projeto finalizado → bloqueado (RN-CB-003).
- `TarefaEtapaHistorico` inicial criado corretamente na etapa de menor ordem.
- Exclusão de workflow/etapa/raia com tarefa ativa vinculada passa a ser efetivamente bloqueada (stub de TASK-03.2/TASK-03.3 substituído pela checagem real).
