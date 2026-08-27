# TASK-02.3 — Regras avançadas de tarefa: edição travada, atribuição, finalização e auditoria [M]

**Status:** Concluída — 2026-08-24

**Epic:** EPIC-02 — Tarefas, Board e Tempo Real | **User Story:** US-02.1 — Gestão de tarefas e movimentação no board
**Sistema:** CRUDAO | **RF:** RF-003, RF-012, RF-017 | **Dependências:** TASK-04.2, TASK-02.1, TASK-01.3

---

## Contexto

PRD v1.2 introduziu regras de negócio novas sobre a Tarefa que dependem do RBAC por projeto (TASK-04.2): trava de edição após a tarefa "iniciada" (RN-009, RN-010), permissão dedicada para chegar/sair da etapa final (RN-011), regras de autoatribuição vs. atribuição a terceiro (RN-012), e histórico de auditoria genérico (RF-017).

## O que deve ser feito

- [ ] Implementar `AuditoriaTarefa` (campo, valor_anterior, valor_novo, usuario_id, criado_em) com índice `idx_auditoria_tarefa (tarefa_id, criado_em)`
- [ ] Trava de edição (RN-009, RN-010): tarefa é "iniciada" assim que sai da etapa inicial do workflow pela primeira vez (permanece iniciada mesmo se retornar); `dev` sem o toggle `devPodeEditarTarefaIniciada` (TASK-01.3) recebe 403 ao editar título/descrição/tipo de tarefa iniciada. `product_owner`/`project_admin`/`admin` sem essa restrição
- [ ] Exclusão de tarefa: `dev` só se o toggle `devPodeExcluirTarefa` estiver ligado; demais papéis com `tarefa:gerenciar` sempre podem
- [ ] Nova permissão `tarefa:finalizar` (RN-011): endpoint de mover tarefa exige essa permissão especificamente quando a transição tem como destino etapa `etapaFinal=true`, na ida e na volta ("desfinalizar", RF-012)
- [ ] Nova permissão `tarefa:atribuir` + endpoint `PATCH /api/tarefas/{id}/responsavel` `{ usuarioId }` (RN-012): se `usuarioId` == usuário autenticado ("puxar"), qualquer membro do projeto pode, mesmo já atribuída a outro, sem aprovação; se `usuarioId` != autenticado, exige `tarefa:atribuir`
- [ ] Toda troca de responsável, edição de título/descrição e mudança de etapa grava `AuditoriaTarefa` na mesma transação da alteração (nunca assíncrono)
- [ ] Endpoint `GET /api/tarefas/{id}/historico` — lista ordenada por `criado_em` desc, 403 se sem acesso ao projeto da tarefa

## Guia técnico

- Pacote: `domain/tarefa`
- Referência: `docs/techspec/kanban-configuravel-techspec.md` (v1.2, seção 4), `data-model.md` (v1.2, `AuditoriaTarefa`), PRD RN-009 a RN-012, RN-016, RF-017

## Critérios de aceite

- Dev não edita título/descrição de tarefa iniciada sem o toggle ligado (teste de integração)
- Dev sem `tarefa:finalizar` recebe 403 ao mover tarefa para etapa final; product_owner com a permissão consegue
- Dev "puxa" tarefa já atribuída a outro dev sem aprovação; tentativa de atribuir a um terceiro sem `tarefa:atribuir` recebe 403
- Toda troca de responsável gera linha em `AuditoriaTarefa` com autor, valor anterior e novo
- `GET /historico` reflete a mudança imediatamente após a operação

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
