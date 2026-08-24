# Data Model — Criação e gerenciamento de cards no board
_Versão: 1.1 | Data: 2026-08-24_

Nenhuma entidade nova, nenhuma tabela nova. Uma coluna nova em `Tarefa` (soft-delete) e um valor novo no enum `TipoEventoBoard`. Consome `Projeto`, `Workflow`, `Etapa`, `Raia` exatamente como já modeladas em `docs/techspec/kanban-configuravel/data-model.md`.

## Alteração — `Tarefa.excluidaEm` (soft-delete)

**Achado do comitê de análise (architect + security + database, 2026-08-24) e decisão do usuário:** `TarefaService.excluir` hoje só limpa `Observador` antes de `tarefaRepository.delete(tarefa)`. `RegistroEtapa`, `Impedimento` (filho de `RegistroEtapa`) e `AuditoriaTarefa` referenciam `tarefa_id` como `@ManyToOne(optional = false)`, sem `ON DELETE CASCADE` — como toda tarefa tem `RegistroEtapa` desde a criação, um hard-delete sempre quebraria com `DataIntegrityViolationException`. Escolhido **soft-delete** em vez de cascade (preserva RN-016/auditoria; evita migração de nulabilidade em `AuditoriaTarefa.tarefa_id`).

```
backend/.../domain/tarefa/Tarefa.java
  + private Instant excluidaEm;   // nullable, default null — soft-delete marker
```

Nenhuma migração de dados necessária além do `ALTER TABLE ADD COLUMN` (Hibernate `ddl-auto=update`, mesmo padrão já usado nas colunas booleanas/nullable anteriores do sistema).

**Efeito em `TarefaRepository`:**

```
+ List<Tarefa> findByProjetoIdAndExcluidaEmIsNullOrderByCriadoEmAsc(UUID projetoId);
```

`listarPorProjeto` (usado por `GET /api/tarefas?projetoId=`, consumido pelo board) passa a usar este método em vez de `findByProjetoIdOrderByCriadoEmAsc`. `buscar(id)` (`GET /api/tarefas/{id}`) passa a lançar `RecursoNaoEncontradoException` quando `tarefa.getExcluidaEm() != null` — a API trata a tarefa como inexistente, mesmo com a linha ainda no banco.

**Entidades relacionadas (`RegistroEtapa`, `Impedimento`, `AuditoriaTarefa`, `Observador`):** nenhuma alteração — continuam referenciando a `Tarefa` normalmente; nada é apagado ou modificado nelas pela exclusão. `AuditoriaTarefa.historicoPorTarefa` continua funcionando sem alteração (histórico da tarefa excluída fica preservado, consultável por quem tiver o `id`, ainda que a tarefa não apareça mais em listagens).

## Alteração — `TipoEventoBoard` (enum, não persistido)

```
backend/.../realtime/TipoEventoBoard.java
  TAREFA_CRIADA
  TAREFA_MOVIDA
  IMPEDIMENTO_ALTERADO
  + TAREFA_EXCLUIDA   // novo
```

Trafega só via payload `pg_notify` (`NotificacaoMinima`) e mensagem STOMP (`EventoBoardDTO`) — nunca gravado em tabela.

## `EventoBoardDTO` — sem alteração de forma

Graças ao soft-delete, a linha da `Tarefa` continua existindo no banco após a exclusão — `PostgresNotificationListener.montarEvento` monta o `EventoBoardDTO` para `TAREFA_EXCLUIDA` exatamente como para os demais tipos (`tarefaRepository.findById` encontra a tarefa normalmente). **Nenhum campo se torna nullable, nenhum caso especial no listener.**

Espelho no frontend (`frontend/src/lib/api/types.ts`) — única mudança é o novo valor de `tipo`:

```
export type TipoEventoBoard = 'TAREFA_CRIADA' | 'TAREFA_MOVIDA' | 'IMPEDIMENTO_ALTERADO' | 'TAREFA_EXCLUIDA';
```

`EventoBoard` (demais campos) permanece sem alteração.

## Entidades reutilizadas (sem alteração)

- `Projeto`, `Workflow`, `Etapa`, `Raia` — usadas só para resolver etapa/raia padrão no frontend (TechSpec §2, D-04); nenhum campo novo.
- `ConfiguracaoProjeto` — `devPodeExcluirTarefa` já existe (TASK-01.3), reutilizado para gating do ícone de lixeira.
- `UsuarioMe` / `ProjetoPapeis` — reutilizados para gating do botão "Novo card" e do ícone de lixeira (permissão `tarefa:gerenciar`).
