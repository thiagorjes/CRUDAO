# Tasks — Criação e gerenciamento de cards no board
_Versão: 1.0 | Data: 2026-08-24 | Sistema: CRUDAO_
_PRD: docs/prd/criacao-card-board-prd.md (v1.0) | TechSpec: docs/techspec/criacao-card-board-techspec.md (v1.0)_

---

## Grafo de Dependências

```
TASK-01.1 (Backend: soft-delete + evento TAREFA_EXCLUIDA)  ⚡ paralelo com TASK-02.1
TASK-02.1 (Frontend: RBAC gating no BoardApp)               ⚡ paralelo com TASK-01.1
  └── TASK-02.2 [P] (Frontend: criar card)         ⚡ paralelo com TASK-02.3
  └── TASK-02.3 [P] (Frontend: excluir card) ── depende também de TASK-01.1
        └── TASK-03.1 (Testes E2E) ── depende de TASK-02.2 e TASK-02.3
```

---

## Sumário de Epics

| Epic | Título | Tasks | Sistema |
|---|---|---|---|
| EPIC-01 | Backend — soft-delete e evento de exclusão | TASK-01.1 | CRUDAO |
| EPIC-02 | Frontend — criação e exclusão de card no board | TASK-02.1, TASK-02.2, TASK-02.3 | CRUDAO |
| EPIC-03 | Testes E2E | TASK-03.1 | CRUDAO |

Workspace de sistema único — sem seção "Plano Git Multi-Sistema".

---

## EPIC-01 — Backend: soft-delete e evento de exclusão

### TASK-01.1 — Backend: migrar exclusão de tarefa para soft-delete e publicar TAREFA_EXCLUIDA [M]

**Sistema:** CRUDAO | **RF:** RF-002 | **Dependências:** nenhuma | **[P]** com TASK-02.1

**Contexto:** `TarefaService.excluir` hoje faz hard-delete e quebra com violação de FK em qualquer tarefa com histórico (`RegistroEtapa`/`Impedimento`/`AuditoriaTarefa`, `optional=false` sem cascade) — bug pré-existente que RF-002 expõe pela primeira vez via UI. TechSpec D-01/D-02.

**O que deve ser feito:**
- [ ] Adicionar campo `Tarefa.excluidaEm` (`Instant`, nullable, default `null`)
- [ ] `TarefaRepository`: novo método `findByProjetoIdAndExcluidaEmIsNullOrderByCriadoEmAsc`
- [ ] `TarefaService.listarPorProjeto` passa a usar o novo método (exclui soft-deleted)
- [ ] `TarefaService.buscar(id)` lança `RecursoNaoEncontradoException` quando `excluidaEm != null`
- [ ] `TarefaService.excluir`: substituir `tarefaRepository.delete(tarefa)` por `tarefa.setExcluidaEm(Instant.now())` + save; manter validações de RBAC/toggle/projeto finalizado (RN-001/RN-002/RN-003, já implementadas) inalteradas
- [ ] `TarefaService.excluir` publica evento `TAREFA_EXCLUIDA` via `publicarAposCommit`, mesmo padrão de `criar`/`mover`
- [ ] `TipoEventoBoard`: adicionar valor `TAREFA_EXCLUIDA`
- [ ] Confirmar que `PostgresNotificationListener.montarEvento` não precisa de caso especial (linha da tarefa ainda existe — `findById` funciona normalmente)

**Guia técnico:**
- Referência: `backend/src/main/java/com/crudao/kanban/domain/tarefa/{Tarefa,TarefaRepository,TarefaService}.java`, `backend/src/main/java/com/crudao/kanban/realtime/{TipoEventoBoard,PostgresNotificationListener}.java`
- Contrato do evento: `docs/techspec/criacao-card-board/contracts/evento-tarefa-excluida.md`
- Data model: `docs/techspec/criacao-card-board/data-model.md`
- `DELETE /api/tarefas/{id}` mantém contrato HTTP idêntico (204, mesmos 403/404/422) — só a persistência interna muda

**Critérios de aceite:**
- `excluir()` faz soft-delete: `excluidaEm` preenchido, linha continua no banco, `RegistroEtapa`/`Impedimento`/`AuditoriaTarefa`/`Observador` intactos (teste explícito para não regredir ao hard-delete)
- `excluir()` publica `TAREFA_EXCLUIDA` (verificar chamada a `EventoBoardPublisher` com o tipo certo)
- `listarPorProjeto` não retorna tarefa soft-deleted (coberto por teste de integração do controller, não só unitário de Service); `buscar(id)` de tarefa soft-deleted lança `RecursoNaoEncontradoException` (404)
- Operações de escrita sobre tarefa soft-deleted (`mover`, `marcarImpedimento`, `atribuir`, `editar`) retornam 404 — teste unitário cobrindo pelo menos `mover`
- `RealtimeBoardIT` estendido: 2 clientes STOMP reais, um exclui, outro recebe `TAREFA_EXCLUIDA` em até 2s (RNF-001)
- Suíte E2E `board.spec.ts` (kanban-configuravel) revalidada sem regressão — gate de saída desta task
- `mvn test` e `spotless:check` limpos

---

## EPIC-02 — Frontend: criação e exclusão de card no board

### TASK-02.1 — Frontend: RBAC gating no BoardApp (permissão e toggle) [P]

**Sistema:** CRUDAO | **RF:** RF-001, RF-002 | **Dependências:** nenhuma | **[P]** com TASK-01.1

**Contexto:** `BoardApp` ainda não busca `GET /api/usuarios/me` nem `GET /projetos/{id}/configuracao` — pré-requisito para decidir se mostra o botão "Novo card" e o ícone de lixeira. TechSpec D-05.

**O que deve ser feito:**
- [ ] `BoardApp` busca `GET /api/usuarios/me` uma vez e calcula `permissoesProjeto` (Set) para o projeto selecionado, reaproveitando o padrão de `AdminApp`/`MembrosAba`
- [ ] `BoardApp` busca `GET /projetos/{id}/configuracao` para obter `devPodeExcluirTarefa`
- [ ] Expor via contexto/props para `CardTarefa` e para o header do board: `podeGerenciarTarefa` (`tarefa:gerenciar`), `podeExcluirTarefa` (gerenciar + não-dev-tier, ou dev-tier + toggle habilitado — reaproveitar heurística `ehDevTier` da TASK-05.4)

**Guia técnico:**
- Referência: `frontend/src/components/board/BoardApp.tsx`, `frontend/src/components/admin/AdminApp.tsx` (padrão de gating), `frontend/src/lib/api/types.ts` (`UsuarioMe`, `ProjetoPapeis`, `ConfiguracaoProjeto` já existem)
- Heurística `ehDevTier`: reaproveitar de onde já foi extraída na TASK-05.4 (`tarefa:gerenciar` sem `tarefa:atribuir`)

**Critérios de aceite:**
- `BoardApp` calcula corretamente `podeGerenciarTarefa`/`podeExcluirTarefa` para os 3 perfis: `project_admin`, `dev`-tier com toggle habilitado, `dev`-tier com toggle desabilitado
- Trocar de projeto no seletor recalcula os dois booleanos sem reload; falha no fetch da configuração resulta em `podeExcluirTarefa=false` (fallback seguro)
- `tsc --noEmit`, `eslint`, `next build` limpos

---

### TASK-02.2 — Frontend: criar card pelo board (RF-001) [M]

**Sistema:** CRUDAO | **RF:** RF-001 | **Dependências:** TASK-02.1 | **[P]** com TASK-02.3

**Contexto:** Botão "Novo card" no board abrindo modal de criação, com etapa/raia padrão calculadas no frontend (TechSpec D-04).

**O que deve ser feito:**
- [ ] Função pura que resolve etapa padrão ("coluna 0" = etapa de menor `ordem`) e raia padrão (primeira raia do projeto, ou `null` se não houver raia própria) a partir do estado já carregado (`estado.etapas`, `estado.raias` ordenadas por `ordem`)
- [ ] Botão "Novo card" no header do board, visível só quando `podeGerenciarTarefa` (TASK-02.1) e `estado.etapas.length > 0`; desabilitado com tooltip/mensagem se o workflow não tiver etapas (D-04, trade-off aceito)
- [ ] Modal "Novo card" (`ModalNovoCard`): campos título (obrigatório), descrição, tipo (`FEATURE`/`BUG`/`CHORE`) e demais atributos disponíveis; estados idle, erro de validação (título vazio), salvando (loading), sucesso (toast)
- [ ] Ao salvar: `POST /api/tarefas` com `{ projetoId, etapaInicialId, raiaId, tipo, titulo, descricao, responsavelId: null }`; card adicionado localmente na resposta HTTP (sem esperar o próprio evento STOMP — mesmo padrão de `mover()`)
- [ ] Tratar erro 403 (bloqueio por projeto finalizado ou falta de permissão) com `ModalErro` já existente

**Guia técnico:**
- Referência: `frontend/src/components/board/BoardApp.tsx`, `frontend/src/lib/api/{types,client}.ts` (`TarefaRequest` já existe), padrão de modal/toast já usado em `AdminApp`/abas
- D-04 (`docs/techspec/criacao-card-board-techspec.md` §2)

**Critérios de aceite (Gherkin do PRD RF-001):**
- Usuário com `tarefa:gerenciar` cria card com título/descrição/tipo → card criado via `POST /api/tarefas`, aparece no board sem reload, na etapa/raia padrão, sem responsável, com feedback de sucesso
- Usuário sem `tarefa:gerenciar` não vê o botão "Novo card"
- Projeto finalizado → criação bloqueada pelo backend, erro exibido claramente
- Função de resolução de etapa/raia padrão testada isoladamente (Vitest): workflow com etapas, projeto sem raia própria, workflow sem etapas (botão desabilitado)
- Card novo inserido no array local ordenado por `criadoEm` ascendente
- `tsc --noEmit`, `eslint`, `next build` limpos

---

### TASK-02.3 — Frontend: excluir card pelo board (RF-002) [M]

**Sistema:** CRUDAO | **RF:** RF-002 | **Dependências:** TASK-01.1, TASK-02.1 | **[P]** com TASK-02.2

**Contexto:** Ícone de lixeira no card com modal de confirmação, consumindo o novo endpoint (soft-delete) e o novo evento `TAREFA_EXCLUIDA` em tempo real.

**O que deve ser feito:**
- [ ] Ícone de lixeira em `CardTarefa`, sempre visível (não só hover), gated por `podeExcluirTarefa` (TASK-02.1)
- [ ] Modal de confirmação de exclusão (novo componente ou reuso do padrão "Modal de Confirmação" existente) com estado loading no botão "Excluir"
- [ ] Ao confirmar: `DELETE /api/tarefas/{id}`; card removido localmente na resposta 204 (direto, sem esperar o evento)
- [ ] Novo branch em `atualizarTarefaLocal` (`BoardApp`): ao receber evento `TAREFA_EXCLUIDA`, remover a tarefa do estado local por `tarefaId`, com o mesmo guard de projeto (`evento.projetoId === atual.projeto.id`) já aplicado aos demais tipos
- [ ] Atualizar `TipoEventoBoard` no frontend (`frontend/src/lib/api/types.ts`) incluindo `'TAREFA_EXCLUIDA'`
- [ ] Cancelar modal → nenhuma alteração

**Guia técnico:**
- Referência: `frontend/src/components/board/{CardTarefa,BoardApp}.tsx`, `frontend/src/lib/board/realtime.ts` (`conectarBoard`, sem alteração necessária)
- Contrato: `docs/techspec/criacao-card-board/contracts/evento-tarefa-excluida.md`

**Critérios de aceite (Gherkin do PRD RF-002):**
- Usuário com `tarefa:gerenciar` (e toggle habilitado, se `dev`-tier) clica na lixeira → modal de confirmação exibido
- Confirmar exclusão → `DELETE /api/tarefas/{id}`, card desaparece do board
- Usuário sem permissão (ou `dev`-tier com toggle desabilitado) não vê o ícone de lixeira
- Segundo cliente conectado ao mesmo projeto recebe `TAREFA_EXCLUIDA` via STOMP e o card some do seu board sem reload
- Erro 403 (permissão/toggle revogados) ou 404 (já excluído por outro cliente) exibido claramente, não falha silenciosa
- Teste unitário (Vitest): branch `TAREFA_EXCLUIDA` de `atualizarTarefaLocal` remove a tarefa do array por `id`, respeita o guard de projeto, e é no-op seguro se já removida localmente
- `tsc --noEmit`, `eslint`, `next build` limpos

---

## EPIC-03 — Testes E2E

### TASK-03.1 — Testes E2E de criação e exclusão de card [P]

**Sistema:** CRUDAO | **RF:** RF-001, RF-002 | **Dependências:** TASK-02.2, TASK-02.3

**Contexto:** Cobertura E2E dos fluxos completos contra a stack real, seguindo o padrão já configurado na TASK-06.1 (kanban-configuravel).

**O que deve ser feito:**
- [ ] Novo spec `e2e/criacao-exclusao-card.spec.ts` (ou extensão de `board.spec.ts`), seguindo o padrão de fixtures existente (`e2e/fixtures/api.ts`, `e2e/fixtures/login.ts`)
- [ ] Cenário: usuário com `tarefa:gerenciar` cria card pela UI → aparece no board na etapa/raia padrão
- [ ] Cenário: usuário sem `tarefa:gerenciar` não vê o botão "Novo card"
- [ ] Cenário: criação bloqueada em projeto finalizado
- [ ] Cenário: usuário com `tarefa:gerenciar` exclui card pela UI (confirmação + remoção)
- [ ] Cenário: usuário sem permissão (ou `dev`-tier com toggle desabilitado) não vê o ícone de lixeira
- [ ] Cenário: exclusão propaga em tempo real para um segundo cliente conectado (2 contextos de browser/sessão), em até 2s (RNF-001)
- [ ] Cenário: workflow ativo sem etapas → botão "Novo card" desabilitado (D-04)
- [ ] Cenário combinado: criar e depois excluir o mesmo card na mesma sessão

**Guia técnico:**
- Referência: `frontend/e2e/{board.spec.ts,rbac.spec.ts,fixtures/}`, `frontend/playwright.config.ts` (já configurado, `globalSetup` já provisiona usuários)
- Specs rodam contra `docker compose up` real, não sobem/derrubam a stack sozinhas (padrão TASK-06.1)

**Critérios de aceite:**
- Todos os 8 cenários acima passando contra a stack real
- Suíte E2E completa (incluindo os specs de kanban-configuravel) revalidada sem regressão

---

## Backlog Priorizado

1. TASK-01.1 e TASK-02.1 em paralelo (sem dependência entre si)
2. TASK-02.2 e TASK-02.3 em paralelo, assim que TASK-02.1 (e, para 02.3, também TASK-01.1) concluírem
3. TASK-03.1 ao final, após TASK-02.2 e TASK-02.3

---

## Histórico de Revisões

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-08-24 | Thiago Goncalves Cavalcante | Versão inicial — 5 tasks em 3 epics |
