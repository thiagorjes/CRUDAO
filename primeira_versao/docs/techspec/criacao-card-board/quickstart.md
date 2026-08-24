# Quickstart — Criação e gerenciamento de cards no board
_Versão: 1.0 | Data: 2026-08-24_

Guia rápido para `/implement`/`/tdd`. Stack e setup completos já documentados em `docs/techspec/kanban-configuravel/quickstart.md` — aqui só o que é específico desta feature.

## Stack (sem mudança)

Backend Java 25/Spring Boot 3.5.16 em `systems/CRUDAO/backend`; frontend Next.js em `systems/CRUDAO/frontend`. `docker compose up -d --build` na raiz do workspace sobe a stack completa (Keycloak, Postgres, backend, frontend).

## Estrutura de pastas tocada

```
systems/CRUDAO/backend/src/main/java/com/crudao/kanban/realtime/
  TipoEventoBoard.java            (+ TAREFA_EXCLUIDA)
systems/CRUDAO/backend/src/main/java/com/crudao/kanban/domain/tarefa/
  Tarefa.java                      (+ campo excluidaEm, Instant nullable — soft-delete)
  TarefaRepository.java            (+ findByProjetoIdAndExcluidaEmIsNullOrderByCriadoEmAsc)
  TarefaService.java               (excluir: soft-delete em vez de tarefaRepository.delete;
                                     publicarAposCommit(TAREFA_EXCLUIDA, ...); listarPorProjeto usa o
                                     novo método do repository; buscar lança 404 se excluidaEm != null)

systems/CRUDAO/frontend/src/lib/api/types.ts        (+ 'TAREFA_EXCLUIDA' em TipoEventoBoard)
systems/CRUDAO/frontend/src/components/board/
  BoardApp.tsx        (botão "Novo card" + gating, branch TAREFA_EXCLUIDA em atualizarTarefaLocal,
                        busca UsuarioMe + ConfiguracaoProjeto)
  CardTarefa.tsx       (ícone de lixeira + gating)
  ModalNovoCard.tsx    (novo)
  ModalConfirmarExclusao.tsx (novo, ou reuso do padrão de ModalErro — decidir na implementação)
```

## Setup mínimo

Nenhum setup novo — endpoints, RBAC, toggle e canal STOMP já existem e já estão provisionados desde as tasks anteriores (TASK-02.1, 02.3, 04.2, 05.1). `docker compose up -d --build` é suficiente.

## Cenários principais (Dado/Quando/Então)

### RF-001 — Criar card

**Dado** que estou no board de um projeto e tenho `tarefa:gerenciar` nele
**Quando** clico em "Novo card", preencho título (obrigatório) e salvo
**Então** `POST /api/tarefas` é chamado com `etapaInicialId` = etapa de menor `ordem` do workflow ativo e `raiaId` = raia de menor `ordem` do projeto (ou `null` se o projeto não tem raia própria); o card aparece no board sem reload; toast de sucesso.

Exemplo de payload:
```json
{ "projetoId": "...", "etapaInicialId": "...", "raiaId": "...", "tipo": "FEATURE", "titulo": "Ajustar X", "descricao": null, "responsavelId": null }
```

**Dado** que não tenho `tarefa:gerenciar` no projeto → botão não aparece (gating via `GET /usuarios/me`).
**Dado** que o projeto está finalizado → backend responde 403/422 (RN-015 já implementada); modal exibe o erro, card não é criado.

### RF-002 — Excluir card

**Dado** que tenho `tarefa:gerenciar` (e, se `dev`-tier, o toggle `devPodeExcluirTarefa` habilitado)
**Quando** clico na lixeira do card e confirmo no modal
**Então** `DELETE /api/tarefas/{id}` é chamado; card some do board imediatamente (quem executou) e em até 2s para os demais clientes conectados (evento `TAREFA_EXCLUIDA`, ver `contracts/evento-tarefa-excluida.md`).

**Dado** que não tenho a permissão (ou sou `dev`-tier sem o toggle) → ícone não aparece.

## Pontos de atenção

1. **`TarefaService.excluir` passa a ser soft-delete, não hard-delete.** Achado do comitê de análise: `RegistroEtapa`/`Impedimento`/`AuditoriaTarefa` têm FK `optional=false` para `tarefa_id` sem cascade — `tarefaRepository.delete(tarefa)` quebraria com `DataIntegrityViolationException` em qualquer tarefa com histórico (= toda tarefa). Trocar por `tarefa.setExcluidaEm(Instant.now()); tarefaRepository.save(tarefa);`. **Não reintroduzir o `delete()` físico.**
2. **Toda leitura de tarefa precisa passar a filtrar `excluidaEm IS NULL`** — `listarPorProjeto` (novo método do repository) e `buscar(id)` (lança 404 se soft-deleted). Não esquecer nenhum ponto de leitura que hoje usa `TarefaRepository` diretamente para listar tarefas de um projeto.
3. **Etapa/raia padrão são calculadas no frontend** (D-04) a partir dos arrays já carregados por `BoardApp` — não inventar lógica de default no backend.
4. Se o workflow ativo não tiver etapas, não há "coluna 0" — desabilitar o botão "Novo card" nesse estado (o board já trata "sem etapas configuradas" separadamente).
5. Gating de UI é só estético (RNF-003/ADR-006) — não pular a validação de permissão no backend achando que o frontend já filtrou.

## Cenários de teste críticos

- `excluir()` faz soft-delete: `excluidaEm` preenchido, `RegistroEtapa`/`Impedimento`/`AuditoriaTarefa`/`Observador` continuam intactos no banco após a exclusão (teste que teria pegado o bug de FK antes de chegar em produção).
- `listarPorProjeto` não retorna tarefa soft-deleted; `buscar(id)` de tarefa soft-deleted lança `RecursoNaoEncontradoException`.
- Exclusão + evento `TAREFA_EXCLUIDA` chegando a um segundo cliente STOMP conectado, em até 2s (estender `RealtimeBoardIT`).
- Criação sem raia própria no projeto → card cai no grupo "Tarefas" (`RAIA_SEM_RAIA_ID`) do board, sem erro.
- Botão/ícone ocultos corretamente para usuário sem `tarefa:gerenciar` e para `dev`-tier com toggle desligado.
