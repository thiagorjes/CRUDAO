# TASK-02.3 — Frontend: excluir card pelo board (RF-002) [M]

**Epic:** EPIC-02 — Frontend: criação e exclusão de card no board
**Sistema:** CRUDAO | **RF:** RF-002 | **Dependências:** TASK-01.1, TASK-02.1 | **[P]** com TASK-02.2

---

## Contexto

Não existe hoje ação de exclusão de card pela UI do board. Esta task adiciona o ícone de lixeira no card e o modal de confirmação, consumindo o `DELETE /api/tarefas/{id}` (agora soft-delete, TASK-01.1) e o novo evento em tempo real `TAREFA_EXCLUIDA` (TASK-01.1) para refletir a exclusão em outros clientes conectados.

## O que deve ser feito

- [ ] Ícone de lixeira em `CardTarefa`, sempre visível (não só em hover), gated por `podeExcluirTarefa` (TASK-02.1)
- [ ] Modal de confirmação de exclusão — novo componente ou reuso do padrão "Modal de Confirmação" já existente (variante de ação destrutiva); estado de loading no botão "Excluir" enquanto a chamada está em andamento
- [ ] Ao confirmar: `DELETE /api/tarefas/{id}`; na resposta 204, remover o card do estado local diretamente (sem esperar o próprio evento STOMP)
- [ ] Tratar erro na exclusão: 403 (permissão/toggle revogados entre a abertura do board e o clique, gating é só estético — RNF-003) e 404 (card já excluído por outro cliente) com `ModalErro`/mensagem clara, mesmo padrão da TASK-02.2 para criação
- [ ] Cancelar o modal: nenhuma alteração de estado
- [ ] Atualizar `TipoEventoBoard` em `frontend/src/lib/api/types.ts` para incluir `'TAREFA_EXCLUIDA'`
- [ ] Novo branch em `BoardApp.atualizarTarefaLocal`: ao receber evento `tipo === 'TAREFA_EXCLUIDA'`, remover a tarefa do array `tarefas` pelo `tarefaId`, com o mesmo guard de projeto (`evento.projetoId === atual.projeto.id`) já aplicado aos demais tipos de evento — ver snippet no contrato

## Guia técnico

- Arquivos a criar/modificar (paths relativos a `systems/CRUDAO/`):
  - `frontend/src/components/board/CardTarefa.tsx` (+ CSS module) — ícone de lixeira
  - `frontend/src/components/board/BoardApp.tsx` — modal de confirmação, chamada `DELETE`, branch `TAREFA_EXCLUIDA` em `atualizarTarefaLocal`
  - `frontend/src/lib/api/types.ts` — `TipoEventoBoard` (+ `'TAREFA_EXCLUIDA'`)
- `conectarBoard` (`frontend/src/lib/board/realtime.ts`) já entrega qualquer `EventoBoard` do tópico — nenhuma mudança necessária nessa função
- Contrato do evento: `docs/techspec/criacao-card-board/contracts/evento-tarefa-excluida.md` (contém o snippet exato do novo branch)

## Critérios de aceite

**Gherkin do PRD (RF-002):**
- Dado que tenho `tarefa:gerenciar` no projeto (e, se `dev`-tier, o toggle `devPodeExcluirTarefa` habilitado), quando clico no ícone de lixeira do card, então um modal de confirmação é exibido
- Dado que o modal de confirmação está aberto, quando confirmo a exclusão, então o card é removido via `DELETE /api/tarefas/{id}` e desaparece do board
- Dado que não tenho a permissão (ou sou `dev`-tier com o toggle desabilitado), quando visualizo o card, então o ícone de lixeira não é exibido

**Outros:**
- Cancelar o modal não altera nada
- Tentar excluir sem permissão (403) ou card já excluído por outro cliente (404) exibe erro claro, não falha silenciosa
- Segundo cliente conectado ao mesmo projeto (STOMP) recebe `TAREFA_EXCLUIDA` e o card some do seu board sem reload — validação de ponta a ponta na TASK-03.1
- Teste unitário (Vitest): branch `TAREFA_EXCLUIDA` de `atualizarTarefaLocal` remove a tarefa correta do array por `id`, não remove nada quando `evento.projetoId` é de outro projeto, e é um no-op seguro se a tarefa já tiver sido removida localmente antes (evento chegando depois da resposta 204 no mesmo cliente que excluiu)
- `tsc --noEmit`, `eslint`, `next build` limpos

---

**Status:** Concluída — 2026-08-24

---

_Origem: [docs/tasks/criacao-card-board-tasks.md](../criacao-card-board-tasks.md)_
