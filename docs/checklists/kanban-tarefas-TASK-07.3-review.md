# Code Review — TASK-07.3 (Detalhe da tarefa)

**Revisor:** agent QA (persona `.agents/agents/qa.md`, skill `/code-review`)
**Data:** 2026-08-26
**Sistema:** CRUDAO (`systems/CRUDAO/frontend/`)
**RF de origem:** RF-003, RF-006, RF-017 (todas Must Have no PRD)

## Gate de testes

`tsc --noEmit` limpo. `npx vitest run` — **39/39 verdes** (9 novos em `lib/tarefa-logic.test.ts`). Gate passou — prosseguindo para revisão.

## Critérios de Aceite

| # | Critério de Aceite | Verificado? | Evidência |
|---|---|---|---|
| 1 | Lead-time exibido bate com o backend, incluindo etapa em andamento | ✅ | `TarefaService.detalhe` (backend, `TarefaService.java:319-328`) calcula `leadTimeSegundos` usando `agora` quando `saidaEm=null`; `TarefaDetalheClient.tsx:203-211` exibe cada item de `historicoEtapas` com sufixo "(em andamento)" quando `saidaEm===null`, ordenado por `ordenarHistoricoEtapas` (entrada asc, testado em `tarefa-logic.test.ts:58-66`) |
| 2 | Campos estruturais desabilitados/bloqueados quando `iniciada=true` | ✅ | `camposEstruturaisBloqueados` (`lib/tarefa-logic.ts:33-35`, testado) alimenta `TarefaDetalheClient.tsx:141-169` — título/descrição viram `.field-locked` somente-leitura; espelha a regra do backend (`TarefaService.editar`, `TarefaService.java:261-266`, 409 se enviado com `iniciada=true`) |
| 3 | Histórico de auditoria em ordem cronológica | ✅ | `ordenarAuditoriaDesc` (testado) — decisão de exibir mais recente primeiro, ordem cronológica válida para trilha de auditoria; renderizado em `TarefaDetalheClient.tsx:265-282`, oculto (não erro) quando `GET /auditoria` retorna 403 (`page.tsx:38-45`), consistente com a exigência de papel gestor/admin do contrato |

## 🔴 Crítico

Nenhum.

## 🟡 Importante

#### I1 — Campo `descricaoEscopo` não pode ser esvaziado via formulário de edição
Arquivo: `systems/CRUDAO/frontend/components/tarefa/TarefaDetalheClient.tsx:60`
Problema: ao salvar, `body.descricaoEscopo = descricaoEscopo.trim() || undefined` — se o usuário apaga todo o texto da descrição para limpá-la, o valor vira `undefined`, que `JSON.stringify` **omite** da requisição. O backend (`EditarTarefaRequest.descricaoEscopo() == null` → campo tratado como "não enviado", `TarefaService.java:278`) não distingue isso de "usuário não tocou no campo" e mantém a descrição antiga. Usuário não consegue limpar uma descrição já preenchida — apenas trocá-la por outro texto não vazio.
Como corrigir:
  Atual:   `body.descricaoEscopo = descricaoEscopo.trim() || undefined;`
  Correto: enviar sempre a string (mesmo vazia) quando `!bloqueado`, ex. `body.descricaoEscopo = descricaoEscopo.trim();` — o backend já aceita string vazia (só checa `!= null`).
Guideline violado: `contracts/tarefas.md` — "PUT /api/tarefas/{id}… iniciada=false: todos os campos" não documenta esvaziamento, mas o comportamento observável diverge do que o campo permite; recomendo também documentar explicitamente no contrato que `descricaoEscopo: ""` é uma edição válida (não coberto — recomendo adicionar).

#### I2 — Nenhum teste de componente para `TarefaDetalheClient` apesar da infraestrutura já existir
Arquivo: `systems/CRUDAO/frontend/components/tarefa/TarefaDetalheClient.tsx` (arquivo inteiro, sem `.test.tsx` correspondente)
Problema: as três RFs desta task (RF-003, RF-006, RF-017) são Must Have no PRD, e `testing.md` exige "cenário de teste correspondente" para toda RF Must Have antes de considerar a task concluída. Só a lógica pura (`tarefa-logic.ts`) tem teste — o componente que efetivamente renderiza o congelamento de campos, o formulário de edição (incl. o bug do I1, que um teste de "limpar descrição" teria pego), a lista de observadores e o histórico de auditoria não tem nenhuma cobertura. Diferente de TASK-07.2 (onde a ausência de testes de componente era aceitável por a infra de Testing Library ainda não existir), esta task roda **depois** da infra já ter sido montada no code review de TASK-07.2 (`@testing-library/react`, `jest-dom`, `jsdom`, `NovoCardModal.test.tsx`/`ConfirmExcluirModal.test.tsx` como padrão de referência) — o custo de replicar o padrão aqui é baixo e o gap já foi sinalizado como guardrail recorrente na dimensão S do canvas ("RFs Must Have exigem cenário de teste automatizado… revisar antes de fechar o Epic 07").
Como corrigir: adicionar `TarefaDetalheClient.test.tsx` cobrindo pelo menos: (a) campos bloqueados quando `tarefa.iniciada=true` (critério de aceite 2), (b) lead-time e "(em andamento)" renderizados a partir de `historicoEtapas` (critério de aceite 1), (c) histórico de auditoria oculto quando `auditoria=null` (403) e renderizado em ordem quando presente (critério de aceite 3).
Guideline violado: `systems/CRUDAO/guidelines/testing.md` — seção "Obrigatoriedade" ("Toda RF Must Have do PRD deve ter cenário de teste correspondente antes de considerar a task concluída").

## 🔵 Sugestão

#### S1 — Dropdown de responsável não reflete a restrição de RN-012 para `dev`
Arquivo: `systems/CRUDAO/frontend/components/tarefa/TarefaDetalheClient.tsx:180-184`
Problema: o `<select>` de responsável lista todos os membros do projeto, mesmo para um usuário `dev` que só pode se autoatribuir (RN-012). O backend revalida corretamente e devolve 403 com mensagem tratada por `mensagemErro`, então não é um problema de segurança — mas a UX permite uma seleção que sempre vai falhar para esse papel. Mesma decisão já aceita em TASK-07.2 (RNF-003: "UI não pré-valida permissão granular… backend sempre revalida", já que `GET /api/me` não expõe `tarefa:gerenciar`) — não bloqueante, mas vale registrar como padrão recorrente a resolver se `GET /me` algum dia expuser permissões efetivas.
Guideline violado: nenhum diretamente — decisão consciente já registrada no canvas.

#### S2 — Novo uso de `<a>` nativo em vez de `next/link`
Arquivo: `systems/CRUDAO/frontend/components/board/BoardClient.tsx:157-162`, `systems/CRUDAO/frontend/app/(shell)/projetos/[id]/tarefas/[tarefaId]/page.tsx:115`
Problema: esta task adiciona duas novas ocorrências de `<a href>` para navegação interna (link do título do card para o detalhe, e "← Board" no cabeçalho do detalhe) — mesmo padrão do gap de lint pré-existente já registrado (`<a>` vs `<Link>`, `next/build` falha por causa disso desde antes desta task, confirmado via `git stash` pelo autor). Não é uma regressão nova de comportamento (SPA navigation ainda funciona via full page load), mas amplia a superfície do débito já conhecido em vez de suspender novas ocorrências até o lint ser corrigido.
Como corrigir: usar `next/link` nos dois pontos assim que o `eslint.config.js` (ESLint 9, gap de TASK-01.1) for restaurado — ou, se for aceito como débito consciente, registrar explicitamente no guardrail do canvas para não se repetir na TASK-07.4+.
Guideline violado: não coberto formalmente (ESLint config ausente) — recomendo adicionar regra explícita em `coding-standards.md` sobre uso de `next/link`.

#### S3 — Título do card no board agora é um link dentro de um elemento `draggable`
Arquivo: `systems/CRUDAO/frontend/components/board/BoardClient.tsx:157-162`
Problema: o `<a>` fica dentro do card com `draggable` (HTML5 DnD, TASK-07.2) — em alguns navegadores, iniciar um drag a partir do próprio texto do link pode disparar o comportamento nativo de "arrastar link" em vez do `onDragStart` customizado do card, ou o clique pode ser interpretado como início de drag e nunca navegar. Comportamento não verificado manualmente (mesmo gap de "sem execução E2E real" já registrado em TASK-07.2).
Guideline violado: nenhum formalmente — sugestão de validação manual antes de fechar o Epic 07.

## ✅ Pontos Positivos

- Reaproveitamento consistente dos padrões já estabelecidos em TASK-07.2 (`apiFetchJson`/`apiProxyFetch`, `forwardToBackend`, `mensagemErro`) em vez de reinventar — reduz superfície de bugs novos.
- Lógica pura de formatação/ordenação/congelamento extraída para `lib/tarefa-logic.ts` desde o início (não como correção de review, diferente de TASK-07.2) — testável sem DOM, com 9 casos cobrindo os três critérios de aceite explicitamente.
- Tratamento correto e documentado do 403 de `GET /auditoria` como "sem acesso" (não erro) em `page.tsx:38-45`, respeitando a exigência de papel gestor/admin do contrato sem quebrar a página para devs comuns.
- `removerResponsavel` como campo explícito (em vez de inferir de `responsavelId=null`) segue corretamente o padrão já fechado em TASK-04.2 para essa ambiguidade.

## Segurança

Nenhum finding de segurança novo. Autorização de leitura/escrita permanece 100% no backend (`PermissaoGuard`/`TarefaService`/`TarefaObservadorService`), como em todas as tasks anteriores do Epic 07 — o frontend só encaminha Bearer server-side via `apiFetch`/`apiProxyFetch`, nunca expõe token ao browser. Não há novos pontos de entrada que aceitem dado não sanitizado antes de ir ao backend (ids vêm de `<select>`/rota, corpo é JSON estruturado). `GET /api/tarefas/{id}/auditoria` tratado como caminho de menor privilégio (403 → oculta seção, não vaza dado).

## Conformidade com TechSpec

Sem desvios da TechSpec/contrato (`contracts/tarefas.md`). Tipos em `lib/tarefa.ts` espelham fielmente `GET /api/tarefas/{id}` e `/auditoria`. Único ponto de atenção é o gap de contrato do I1 (esvaziar `descricaoEscopo`), que é uma lacuna de especificação, não uma violação.

## Resultado

**APROVADO COM RESSALVAS** — 0 críticos, 2 importantes (I1, I2), 3 sugestões não bloqueantes.

## Correções aplicadas — 2026-08-26

- **I1 corrigido:** `TarefaDetalheClient.tsx` agora envia `body.descricaoEscopo = descricaoEscopo.trim()` sem fallback para `undefined` — apagar o campo persiste string vazia em vez de manter o valor antigo.
- **I2 corrigido:** `TarefaDetalheClient.test.tsx` criado (7 casos) — campos bloqueados/editáveis por `iniciada`, lead-time com "(em andamento)", auditoria oculta em 403 e ordenada corretamente, envio de descrição vazia (regressão do I1) e adição de observador.
- Suite completa: `tsc --noEmit` limpo, `vitest run` **46/46 verdes** (17 novos: 9 em `tarefa-logic.test.ts` + 7 em `TarefaDetalheClient.test.tsx`).
- S1/S2/S3 mantidos como débito consciente (mesma decisão do relatório original).
