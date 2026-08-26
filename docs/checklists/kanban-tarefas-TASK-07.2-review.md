# Code Review — TASK-07.2 — Board: colunas, raias, cards, criar/excluir, mover

**Revisor:** agent QA (contexto fresco) — 2026-08-26
**Sistema:** CRUDAO (`systems/CRUDAO/`)

## Gate de testes

- Backend: `mvn -gs settings-no-mirror.xml test -Dmaven.compiler.release=21 -Dtest="*Test"` — **verde** (sem ITs, ambiente sem JDK 25 pinado no pom).
- Frontend: `npx tsc --noEmit` — sem erros. `npx vitest run` — **9/9 verdes** (2 arquivos: `pkce.test.ts`, `session.test.ts`, pré-existentes; nenhum teste novo para os componentes desta task).
- Gate passou → revisão prosseguiu.

## Critérios de Aceite

| # | Critério de Aceite | Verificado? | Evidência |
|---|---------------------|-------------|-----------|
| 1 | Board reflete estado do backend, colunas na ordem configurada, tarefas agrupadas por raia | ✅ | `BoardClient.tsx:31-45` (`etapasOrdenadas`/`raiasOrdenadas` por `ordem`, `tarefasDe` filtra por `raiaId`+`etapaAtualId`) |
| 2 | Criar card na etapa de menor ordem | ✅ | `BoardClient.tsx:41,191-199` (`etapaMenorOrdem`, botão "+ Novo card" só na coluna correspondente); etapa de destino é decidida pelo backend (RN-CB-004/005), frontend não envia `etapaId` — `NovoCardModal.tsx:39-47` |
| 3 | Excluir card | ✅ | `ConfirmExcluirModal.tsx` + `BoardClient.excluir` (`tarefa/[id]/route.ts` DELETE) |
| 4 | Mover card (drag-and-drop) chamando `POST /mover`, feedback de erro em transição bloqueada | ✅ | `BoardClient.tsx:52-63,121-132,243-251` — `onDrop`→`mover`, `mensagemErro` mapeia 409/403 |
| 5 | Indicador visual de impedimento + marcar/desmarcar | ✅ | `BoardClient.tsx:76-85,168` (badge "Impedido", botão alternarImpedimento) |
| 6 | Tempo real via STOMP `/topic/board/{projetoId}`, resincronização por gap de `seq` | ⚠️ | `useBoardRealtime.ts:56-61` — qualquer evento dispara `router.refresh()` (não lê/compara `seq` do payload); decisão documentada no código como equivalente funcional (payload enxuto, "algo mudou, busque de novo"), mas não é o mecanismo de "resincronização por gap de seq" descrito na task/TASK-05.3 — ver finding 🔵 abaixo |
| 7 | Atualização em tempo real sem refresh manual (<2s) | ✅ (não medido) | `useBoardRealtime.ts:56-61` chama `router.refresh()` no `onConnect` e em toda mensagem do tópico; sem teste automatizado de latência (esperado — depende de ambiente real) |
| 8 | Reconexão de WebSocket dispara resincronização via `GET /board` | ✅ | `useBoardRealtime.ts:56-61,63-68,75-80` — `onConnect` chama `router.refresh()` também após reconexão (novo ticket a cada tentativa, backoff 1s→30s) |

## 🔴 Crítico

Nenhum.

## 🟡 Importante

> **Atualização pós-review (2026-08-26):** os 3 achados abaixo foram corrigidos a pedido do usuário. Ver `memory/state.md` (handoff TASK-07.2) para o resumo.

#### I1 — Endpoint/mecanismo `POST /api/ws-ticket` não documentado em nenhum contrato — ✅ corrigido
Arquivo: `docs/techspec/kanban-tarefas/contracts/` (pasta inteira — não há `websocket.md` nem menção em `tarefas.md`)
Problema: a task introduz uma peça nova de API pública (`POST /api/ws-ticket`, resposta `{ticket, expiraEm}`) e um novo parâmetro de handshake (`/ws?ticket=...`), mas nenhum arquivo em `contracts/` reflete isso. TASK-04.5 fixou o precedente de manter os contratos como fonte de verdade sincronizada com o código (achado daquela review: `RaiaResponse.global` fora do contrato foi corrigido documentando o campo).
Como corrigir:
  Atual: contrato de STOMP/board não descreve o ticket, o TTL de 20s nem o parâmetro de query.
  Correto: adicionar seção (novo arquivo `contracts/websocket.md` ou seção em `tarefas.md`) documentando `POST /api/ws-ticket`, TTL, uso único e o parâmetro `?ticket=` do handshake `/ws`.
Guideline violado: não coberto explicitamente por um guideline de "contratos sempre atualizados", mas é convenção já estabelecida em code review anterior (TASK-04.5) — recomendo formalizar em `guidelines/spdd-integration.md` ou similar.

#### I2 — Sem teste automatizado para os componentes novos do board (frontend) — ✅ corrigido
Arquivo: `components/board/{BoardClient,useBoardRealtime,NovoCardModal,ConfirmExcluirModal}.tsx`
Problema: `testing.md` exige "Framework: Jest/Vitest + Testing Library" no frontend e que "Toda RF Must Have do PRD deve ter cenário de teste correspondente antes de considerar a task concluída" — RF-001/002/004/018/019 (Must Have) são exatamente o escopo desta task, mas a suíte vitest não ganhou nenhum arquivo novo (`vitest run` continua em 9/9, os mesmos 2 arquivos pré-existentes de TASK-07.1). Lógica não trivial sem cobertura: `transicaoPermitida`, `mensagemErro` (mapeamento 409/403), fluxo de criar/excluir/mover, e o backoff/reconexão de `useBoardRealtime`.
Como corrigir:
  Atual: nenhum teste para `BoardClient`/`useBoardRealtime`/modais.
  Correto: ao menos testes de unidade para as funções puras extraíveis (`transicaoPermitida`, `mensagemErro`, `classeDestaque`) e um teste de integração de componente (Testing Library) cobrindo criar/excluir/mover com mocks de `fetch`.
Guideline violado: `guidelines/testing.md` — seções "Frontend" e "Obrigatoriedade".

#### I3 — `ws_ticket` sem rotina de limpeza (crescimento ilimitado da tabela) — ✅ corrigido
Arquivo: `V12__ws_ticket.sql`, `WsTicketService.java`
Problema: cada tentativa de conexão (inicial + toda reconexão do frontend) gera uma linha nova em `ws_ticket` que nunca é removida — `usado=true`/expirado permanece indefinidamente. O índice `idx_ws_ticket_expira_em` sugere que uma rotina de purga era esperada, mas não existe (nem `@Scheduled`, nem job, nem `DELETE` em lote). Em uso normal (reconexões por queda de rede, múltiplas abas) a tabela cresce sem limite.
Como corrigir:
  Atual: nenhuma remoção de tickets expirados/usados.
  Correto: job agendado (`@Scheduled`) ou `DELETE FROM ws_ticket WHERE expira_em < now() - intervalo` periódico, usando o índice já criado.
Guideline violado: não coberto explicitamente — recomendo adicionar a `guidelines/observability.md` ou registrar como débito técnico (ADR) se aceito conscientemente para esta fase.

## 🔵 Sugestão

#### S1 — Resincronização "por gap de seq" não implementada; qualquer evento refaz `GET /board`
Arquivo: `components/board/useBoardRealtime.ts:58-61`
O texto da task e o padrão de TASK-05.3 falam em "resincronização por gap de `seq`" (detectar buracos na sequência e então resincronizar). A implementação simplifica para "todo evento de tópico dispara `router.refresh()`", o que cobre o critério de aceite funcional (o board sempre fica atualizado) com menos código, mas descarta o `seq`/payload do evento por completo. Efeito colateral: sob alta frequência de eventos (múltiplos usuários movendo cards ao mesmo tempo), cada evento dispara um novo fetch completo do board, sem debounce. Não bloqueante — decisão pragmática documentada no próprio código — mas vale um debounce curto (ex.: 300ms) se o volume de eventos em produção se mostrar alto.

#### S2 — `onDrop` não revalida `transicaoPermitida` antes de chamar `mover`
Arquivo: `components/board/BoardClient.tsx:126-132`
A UI depende do `onDragOver` nunca chamar `preventDefault()` em colunas inválidas para o navegador bloquear o evento `drop` nativo. Funciona nos navegadores modernos, mas é um contrato implícito do HTML5 DnD, não uma checagem explícita no handler. O backend já valida (409), então não há risco de segurança/dado — só fragilidade de UI caso o comportamento de drag-and-drop mude entre navegadores. Sugestão: repetir `if (!transicaoPermitida(...)) return;` no início do `onDrop`.

#### S3 — `POST /api/ws-ticket` sem limite de emissão por usuário
Arquivo: `WsTicketController.java`, `WsTicketService.emitir`
Qualquer usuário autenticado pode chamar o endpoint repetidamente sem limite, gerando tickets que nunca serão usados (contribui também para o I3). TTL de 20s e uso único já mitigam abuso como vetor de autenticação, mas não como vetor de esgotamento de tabela. Não bloqueante.

## ✅ Pontos Positivos

- A resolução do problema "WebSocket API nativa do browser não envia header Authorization" com um ticket de uso único + TTL de 20s é elegante e documentada com clareza no código (`WsTicketAuthenticationFilter`, `env.ts`) — a decisão arquitetural (validada com architect+security) está bem justificada nos comentários, facilitando a review sem precisar reconstruir o raciocínio.
- `WsTicketService.validarEUsar` marca o ticket como usado mesmo quando expirado, fechando de propósito a janela de reuso — coberto por teste dedicado (`validarEUsar_ticketExpirado_mesmoAssimEMarcadoComoUsado_naoPodeSerReaproveitado`).
- `AtivoUsuarioFilter` não interfere na autenticação por ticket (só atua quando `Authentication` é `AbstractOAuth2TokenAuthenticationToken`), e seu `finally { clear() }` roda só depois que o handshake WS já consumiu o `UsuarioAutenticadoHolder` — a ordem dos filtros (`addFilterBefore`/`addFilterAfter`) foi pensada para não quebrar esse fluxo.
- Separação clara entre `apiFetch` (SSR, com `redirect()`) e `apiProxyFetch`/`forwardToBackend` (mutações client-side, sem `redirect()`, devolvendo status real ao browser) evita o erro comum de um `redirect()` disparado dentro de um Route Handler chamado via `fetch()` do cliente.
- `NovoCardModal`/`ConfirmExcluirModal` cuidam de acessibilidade básica (`role="dialog"`/`role="alertdialog"`, `aria-modal`, `aria-describedby`, `aria-busy`) sem terem sido pedidos explicitamente no critério de aceite.

## Segurança

- Autenticação do handshake WS via ticket de uso único, TTL de 20s, validado e invalidado atomicamente em uma única transação (`WsTicketService.validarEUsar`) — sem janela de corrida óbvia para reuso.
- `WsTicketAuthenticationFilter` verifica `usuario.isAtivo()` antes de autenticar a sessão WS — usuário desativado não consegue abrir o board em tempo real mesmo com ticket válido.
- `BoardChannelInterceptor` (já existente, reconfirmado nesta review) continua aplicando RBAC por projeto (`permissaoGuard.membro`) na subscrição ao tópico — ticket novo não contorna essa checagem, só resolve a autenticação do handshake.
- Rotas proxy do Next.js (`app/api/**`) nunca expõem o `accessToken` ao corpo/headers de resposta — repassam só o corpo/status do backend.
- Nenhum secret hardcoded encontrado nos arquivos revisados.
- Achados de segurança sem risco imediato: I3 e S3 (esgotamento de tabela/sem rate limit) são disponibilidade, não confidencialidade/integridade — mantidos como Importante/Sugestão, não Crítico.

## Conformidade com TechSpec

- Ver I1 — mecanismo de ticket não documentado nos contratos formais (`docs/techspec/kanban-tarefas/contracts/`).
- Fora esse gap documental, a implementação é consistente com a decisão arquitetural registrada em `memory/state.md`/canvas (Bearer nunca no browser, ticket de curta duração para o handshake) e com ADR-004 (broadcast via LISTEN/NOTIFY — não alterado nesta task, só consumido).

## Resultado

**APROVADO COM RESSALVAS**

Nenhum bloqueador de merge (🔴). Os 3 achados 🟡 (I1, I2, I3) devem ser resolvidos ou conscientemente aceitos como débito técnico (registrar ADR se for o caso) antes de considerar o Epic 07 fechado — em particular I2 (falta de teste automatizado) é o mais relevante frente à obrigatoriedade explícita de `testing.md` para RFs Must Have.
