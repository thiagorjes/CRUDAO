# TASK-02.2 — Frontend: criar card pelo board (RF-001) [M]

**Epic:** EPIC-02 — Frontend: criação e exclusão de card no board
**Sistema:** CRUDAO | **RF:** RF-001 | **Dependências:** TASK-02.1 | **[P]** com TASK-02.3

---

## Contexto

Não existe hoje forma de criar uma tarefa pela UI do board — ficou explicitamente fora do escopo da TASK-05.1. Esta task adiciona o botão "Novo card" e o modal de criação, com etapa/raia padrão ("coluna 0"/primeira raia) calculadas no frontend a partir do estado já carregado pelo `BoardApp` (TechSpec D-04) — sem endpoint novo, sem alteração no backend.

## O que deve ser feito

- [ ] Função pura que resolve etapa padrão (etapa de menor `ordem` do workflow ativo — "coluna 0") e raia padrão (raia de menor `ordem` do projeto, ou `null` se o projeto não tiver raia própria) a partir de `estado.etapas`/`estado.raias` já carregados e ordenados
- [ ] Botão "Novo card" no header do board:
  - visível só quando `podeGerenciarTarefa` (TASK-02.1) é `true`
  - desabilitado (com mensagem/tooltip explicando o motivo) quando `estado.etapas.length === 0` (trade-off D-04 — sem "coluna 0" não há default possível)
- [ ] Modal "Novo card" (`ModalNovoCard`, novo componente): campos título (obrigatório), descrição, tipo (`FEATURE`/`BUG`/`CHORE`) e demais atributos disponíveis na entidade Tarefa; estados idle, erro de validação (título vazio, bloqueia o envio), salvando (loading), sucesso (fecha o modal + toast)
- [ ] Ao salvar: `POST /api/tarefas` com `{ projetoId, etapaInicialId, raiaId, tipo, titulo, descricao, responsavelId: null }`; na resposta HTTP, adicionar o card ao estado local diretamente (sem esperar o próprio evento STOMP — mesmo padrão de atualização otimista/direta já usado em `mover()`)
- [ ] Tratar erro 403 (bloqueio por projeto finalizado, ou falta de permissão caso o gating de UI tenha ficado dessincronizado) com o `ModalErro` já existente, mensagem clara

## Guia técnico

- Arquivos a criar/modificar (paths relativos a `systems/CRUDAO/`):
  - `frontend/src/components/board/BoardApp.tsx` (botão, integração do modal, atualização de estado)
  - `frontend/src/components/board/ModalNovoCard.tsx` (+ CSS module) — novo
  - `frontend/src/lib/board/*.ts` — nova função pura de resolução de defaults (D-04), local a definir seguindo a convenção de `frontend/src/lib/board/` já usada por `transicoes.ts`/`agrupar.ts`/`tempo.ts`
- `TarefaRequest` (payload de `POST /api/tarefas`) já existe em `frontend/src/lib/api/types.ts`/`client.ts` — não precisa de alteração
- Referência de decisão técnica: TechSpec D-04 (`docs/techspec/criacao-card-board-techspec.md` §2)

## Critérios de aceite

**Gherkin do PRD (RF-001):**
- Dado que estou no board de um projeto e tenho `tarefa:gerenciar`, quando clico em "Novo card", preencho título/descrição/tipo e salvo, então o card é criado via `POST /api/tarefas`, aparece imediatamente no board (sem reload) na etapa/raia padrão, sem responsável, com feedback de sucesso
- Dado que não tenho `tarefa:gerenciar` no projeto, quando visualizo o board, então o botão "Novo card" não é exibido
- Dado que o projeto está finalizado, quando tento criar um card, então a ação é bloqueada pelo backend e o erro é exibido claramente

**Outros:**
- Função de resolução de etapa/raia padrão testada isoladamente (Vitest): workflow com etapas normal, projeto sem raia própria (retorna raia `null`), workflow sem etapas (nenhuma etapa padrão — botão fica desabilitado)
- Formulário bloqueia o envio e sinaliza o campo obrigatório quando título não é preenchido
- Card recém-criado é inserido no array local ordenado por `criadoEm` ascendente, consistente com a ordem que um refetch completo (ou a chegada de `TAREFA_CRIADA` em outro cliente) produziria
- `tsc --noEmit`, `eslint`, `next build` limpos

---

**Status:** Concluída

---

_Origem: [docs/tasks/criacao-card-board-tasks.md](../criacao-card-board-tasks.md)_
