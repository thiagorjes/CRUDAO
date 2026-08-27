# TASK-04.4 — Excluir tarefa + leitura de auditoria

**Status:** Concluída — 2026-08-25
**Tamanho:** [M] 4-8h
**Sistema:** CRUDAO
**RF de origem:** RF-019, RF-017
**Dependências:** TASK-04.1, TASK-02.3
**Paralelismo:** [P] com TASK-04.2, TASK-04.3

## Contexto

Fecha o RF-019 (exclusão de card pelo board) e consolida a leitura da estrutura de auditoria (`TarefaAuditoria` já gravada em TASK-04.2/TASK-04.3). **Modelagem do toggle `devPodeExcluirTarefa` decidida pelo Comitê de Análise:** permissão dedicada `tarefa:excluir` no catálogo `Permissao` (não flag de contexto sobre `tarefa:gerenciar`) — segue o mesmo padrão de toggle já usado por todas as demais permissões via `PapelPermissao`, evitando um mecanismo de exceção paralelo. Default: `habilitada=false` para papel `dev`, `true` para os demais papéis com `tarefa:gerenciar`. Esse default deve ser adicionado ao seed de V2 (TASK-01.2) ou documentado como ajuste de seed nesta task se V2 já estiver aplicada.

## O que deve ser feito

- [x] Implementar `DELETE /api/tarefas/{id}`: exige `tarefa:gerenciar` (RN-CB-001); se usuário é `dev`, exige adicionalmente `tarefa:excluir` habilitada (RN-CB-002); bloqueado se projeto finalizado (RN-CB-003).
- [x] Emitir evento `TAREFA_EXCLUIDA` (consumido por TASK-05.1) e refletir remoção nos boards abertos em até 2s (RNF-001) — publicação real fica a cargo do publisher de TASK-05.1 (mesmo estado dos demais endpoints de `TarefaService`).
- [x] Implementar `GET /api/tarefas/{id}/auditoria` (RF-017) retornando histórico completo (autor, valor anterior/novo, data/hora) agregando os registros gravados em TASK-04.2/TASK-04.3 e nesta task (campo `responsavel`, `titulo` também cobertos aqui se ainda pendentes).
- [x] Adicionar/confirmar a permissão `tarefa:excluir` no catálogo `Permissao` e seus defaults por papel (referenciar migration V2 de TASK-01.2 — **não criar nova migration aqui**, a migration V8 é de TASK-02.3). Confirmado já presente na V2. Permissão adicional `tarefa:auditoria` criada na migration V10 (achado de code review, agent QA — `GET /auditoria` exige "papel gestor ou admin", não representável por `tarefa:gerenciar`).

## Guia técnico

- `backend/src/main/java/.../tarefa/TarefaService.java` (método `excluir`).
- `backend/src/main/java/.../tarefa/TarefaAuditoriaService.java`.
- Contrato: `docs/techspec/kanban-tarefas/contracts/tarefas.md` (seção DELETE, GET auditoria).

## Critérios de aceite

- Exclusão bloqueada para dev com `tarefa:excluir` desabilitada; permitida com toggle habilitada.
- Exclusão bloqueada em projeto finalizado.
- `GET /auditoria` retorna todas as alterações relevantes (responsável, título, etapa, impedimento) com autor/valores/data.
