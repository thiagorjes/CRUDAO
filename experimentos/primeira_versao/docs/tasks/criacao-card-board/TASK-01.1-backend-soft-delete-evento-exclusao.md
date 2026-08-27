# TASK-01.1 — Backend: migrar exclusão de tarefa para soft-delete e publicar TAREFA_EXCLUIDA [M]

**Epic:** EPIC-01 — Backend: soft-delete e evento de exclusão
**Sistema:** CRUDAO | **RF:** RF-002 | **Dependências:** nenhuma | **[P]** com TASK-02.1

---

## Contexto

`TarefaService.excluir` hoje faz hard-delete (`tarefaRepository.delete(tarefa)`) e quebra com violação de FK em qualquer tarefa com histórico — `RegistroEtapa`, `Impedimento` (filho de `RegistroEtapa`) e `AuditoriaTarefa` referenciam `tarefa_id` como `@ManyToOne(optional = false)`, sem `ON DELETE CASCADE`. Como toda tarefa tem `RegistroEtapa` desde a criação, isso é um bug pré-existente que RF-002 expõe pela primeira vez (é a primeira feature a exercitar `DELETE /api/tarefas/{id}` de ponta a ponta pela UI). Decisão do usuário (comitê architect/security/database): **soft-delete** — preserva RN-016/auditoria sem exigir cascade nem migração de nulabilidade.

Também não existe hoje evento de broadcast `TAREFA_EXCLUIDA` — só `TAREFA_CRIADA`/`TAREFA_MOVIDA`/`IMPEDIMENTO_ALTERADO` existem. Graças ao soft-delete, a linha da tarefa continua existindo após a exclusão, então o listener não precisa de nenhum caso especial para montar o evento.

## O que deve ser feito

- [ ] Adicionar campo `Tarefa.excluidaEm` (`Instant`, nullable, default `null`)
- [ ] `TarefaRepository`: novo método `findByProjetoIdAndExcluidaEmIsNullOrderByCriadoEmAsc`
- [ ] `TarefaService.listarPorProjeto` passa a usar o novo método (exclui tarefas soft-deleted da listagem)
- [ ] `TarefaService.buscar(id)` lança `RecursoNaoEncontradoException` quando `tarefa.getExcluidaEm() != null` (API trata como inexistente)
- [ ] `TarefaService.excluir`: substituir `tarefaRepository.delete(tarefa)` por `tarefa.setExcluidaEm(Instant.now())` + `tarefaRepository.save(tarefa)`; manter inalteradas as validações de RBAC/toggle/projeto finalizado (RN-001/RN-002/RN-003, já implementadas desde TASK-02.1/02.3/04.2) e a limpeza de `Observador` já existente antes da exclusão (agora antes do soft-delete, se ainda fizer sentido — reavaliar se `Observador` deve continuar sendo limpo dado que a tarefa não é mais fisicamente apagada; se não houver razão técnica para limpar, preservar como está para não perder o histórico de quem observava)
- [ ] `TarefaService.excluir` publica evento `TAREFA_EXCLUIDA` via `publicarAposCommit`/`EventoBoardPublisher`, mesmo padrão de `criar`/`mover` (chamado dentro do `afterCommit` da transação)
- [ ] `TipoEventoBoard` (enum): adicionar valor `TAREFA_EXCLUIDA`
- [ ] Confirmar (sem alteração necessária, só verificação) que `PostgresNotificationListener.montarEvento` monta o `EventoBoardDTO` para `TAREFA_EXCLUIDA` do mesmo jeito que para os demais tipos — `tarefaRepository.findById` encontra a tarefa normalmente, pois a linha ainda existe
- [ ] Confirmar (achado do comitê architect+QA) que `mover`, `marcarImpedimento`/`desmarcarImpedimento` e `atribuir` continuam operando via `buscar(id)`/`buscarEntidade(id)` — como esses métodos passam a herdar o 404 de tarefa soft-deleted "de graça", nenhuma alteração de código deveria ser necessária, mas isso precisa ser verificado explicitamente (ver critério de aceite abaixo) para não haver um caminho de escrita que acesse a entidade sem passar por esse guard

## Guia técnico

- Arquivos a modificar (paths relativos a `systems/CRUDAO/`):
  - `backend/src/main/java/com/crudao/kanban/domain/tarefa/Tarefa.java` — novo campo `excluidaEm`
  - `backend/src/main/java/com/crudao/kanban/domain/tarefa/TarefaRepository.java` — novo método de busca
  - `backend/src/main/java/com/crudao/kanban/domain/tarefa/TarefaService.java` — `excluir`, `listarPorProjeto`, `buscar`
  - `backend/src/main/java/com/crudao/kanban/realtime/TipoEventoBoard.java` — novo valor de enum
- Contrato do evento: `docs/techspec/criacao-card-board/contracts/evento-tarefa-excluida.md`
- Data model: `docs/techspec/criacao-card-board/data-model.md`
- `DELETE /api/tarefas/{id}` mantém contrato HTTP idêntico (204, mesmos 403/404/422 já documentados) — só a persistência interna muda; `POST /api/tarefas` não é alterado
- Migração de schema: `ALTER TABLE ADD COLUMN` via Hibernate `ddl-auto=update` (mesmo padrão já usado em colunas nullable anteriores) — nenhuma migração de dados manual necessária

## Critérios de aceite

- `excluir()` faz soft-delete: `excluidaEm` preenchido com timestamp, a linha continua no banco, `RegistroEtapa`/`Impedimento`/`AuditoriaTarefa`/`Observador` permanecem intactos (teste unitário explícito, para não regredir ao hard-delete que quebrava por FK)
- `excluir()` publica `TAREFA_EXCLUIDA` (teste verifica chamada a `EventoBoardPublisher` com o tipo correto)
- `listarPorProjeto` não retorna tarefas soft-deleted — coberto por teste de integração do controller (`GET /api/tarefas?projetoId=` real, não só `TarefaServiceTest` isolado), já que é a rota consumida pelo board (RF-002 — "card desaparece do board")
- `buscar(id)` de tarefa soft-deleted lança `RecursoNaoEncontradoException` (404 na API)
- Qualquer operação de escrita sobre tarefa soft-deleted (`mover`, `marcarImpedimento`, `desmarcarImpedimento`, `atribuir`, `editar`) retorna 404 — teste unitário explícito cobrindo pelo menos `mover` sobre tarefa já excluída
- `RealtimeBoardIT` estendido com cenário de exclusão: 2 clientes STOMP reais, um exclui a tarefa, o outro recebe o evento `TAREFA_EXCLUIDA` em até 2s (RNF-001)
- Suíte E2E de `board.spec.ts` (kanban-configuravel) revalidada sem regressão após a mudança de fonte de leitura em `listarPorProjeto`/`buscar` — gate de saída desta task, antes de liberar TASK-02.3
- `mvn test` e `spotless:check` limpos

**Débito técnico registrado (não bloqueante):** índice composto `(projeto_id, excluida_em)` não avaliado nesta task — anotar se o volume de dados justificar em task futura.

---

**Status:** Concluída — 2026-08-24

---

_Origem: [docs/tasks/criacao-card-board-tasks.md](../criacao-card-board-tasks.md)_
