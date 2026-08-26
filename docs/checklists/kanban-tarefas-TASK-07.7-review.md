# Code Review — TASK-07.7 (Notificações UI)

_Revisor: agent QA — 2026-08-26_

## Gate de testes

- `npx tsc --noEmit` (systems/CRUDAO/frontend/) — limpo, sem erros.
- `npx vitest run` (systems/CRUDAO/frontend/) — **112/112 verdes** (21 arquivos), incl. os novos `lib/notificacoes-logic.test.ts` (2 casos) e `components/notificacoes/NotificacoesBell.test.tsx` (3 casos).

Gate passou — revisão prossegue nas 5 categorias.

## Critérios de Aceite

| # | Critério de Aceite | Verificado? | Evidência |
|---|---|---|---|
| 1 | Notificação aparece em tempo real quando o usuário é observador de uma tarefa alterada | ✅ | `components/notificacoes/useNotificacoesRealtime.ts:59-67` (subscrição `/topic/notificacoes/{usuarioId}`) + `components/notificacoes/NotificacoesBell.tsx:34-36` (`mesclarNotificacao` insere no estado) |
| 2 | Marcar como lida reflete no backend e na UI | ✅ | `components/notificacoes/NotificacoesBell.tsx:38-46` (update otimista + `POST /api/notificacoes/{id}/lida` + revert em falha) |

## 🔴 Crítico

Nenhum.

## 🟡 Importante

#### I1 — Proxy `GET /api/notificacoes` descarta a query string `?apenasNaoLidas=true`
Arquivo: `app/api/notificacoes/route.ts:5-7`
Problema: `NotificacoesBell.tsx:21` chama `fetch("/api/notificacoes?apenasNaoLidas=true")`, mas o proxy repassa a chamada via `forwardToBackend(req, "/api/notificacoes", "GET")` — um caminho fixo, sem a query string do request original. `forwardToBackend` não repassa query string sozinho (mesmo comportamento documentado e contornado explicitamente em `app/api/projetos/[id]/usuarios/buscar/route.ts`, TASK-07.5). O backend nunca recebe `apenasNaoLidas=true`.
Impacto atual mitigado, mas latente: `memory/state.md` (achado de TASK-05.2) registra que `GET /api/notificacoes` hoje **ignora esse parâmetro no backend e já sempre retorna só não lidas** — por isso não há regressão funcional visível agora, e o filtro client-side (`naoLidas = notificacoes.filter(n => !n.lida)`) mascara o problema. Mas o parâmetro se torna morto dos dois lados (frontend manda, proxy descarta, backend ignora), e se o débito do backend for revisitado (o próprio `state.md` já sinaliza isso como possível) o frontend passará a buscar o histórico completo sem perceber, sem que nenhum teste automatizado detecte a regressão — o teste existente (`NotificacoesBell.test.tsx:32`) só verifica a chamada de `fetch` do browser, nunca o que chega ao backend.
Como corrigir:
  Atual:   `return forwardToBackend(req, "/api/notificacoes", "GET");`
  Correto: montar a query string explicitamente a partir de `req.nextUrl.searchParams`, replicando o padrão já usado em `app/api/projetos/[id]/usuarios/buscar/route.ts`.
Guideline violado: não há guideline formal, mas é um padrão já estabelecido e documentado no próprio código-base uma task antes (TASK-07.5) para exatamente este caso — repetir o padrão evita reintroduzir o mesmo gap.

## 🔵 Sugestão

#### S1 — Painel de notificações não fecha ao clicar fora
Arquivo: `components/notificacoes/NotificacoesBell.tsx:64-81`
O painel (`role="dialog"`) só fecha reclicando no sino — sem listener de clique fora nem tecla Esc. Baixo risco, não bloqueante; mesmo nível de acabamento aceito em outros modais do Epic 07.

#### S2 — Nenhum teste dedicado para `useNotificacoesRealtime`
Arquivo: `components/notificacoes/useNotificacoesRealtime.ts`
Mesma decisão já aceita para `useBoardRealtime` em TASK-07.2 (hook de conexão STOMP/SockJS real não testado isoladamente, mockado nos testes de componente) — consistente, não é um gap novo desta task.

## ✅ Pontos Positivos

- Reforço decorativo client-side do filtro de `usuarioId` no callback STOMP (`useNotificacoesRealtime.ts:65`), mesmo sabendo que a autorização real já é feita no `ChannelInterceptor` do backend — defesa em profundidade barata, consistente com o guardrail já registrado no canvas para o board.
- Update otimista de "marcar como lida" com revert explícito em caso de falha (`NotificacoesBell.tsx:38-46`), evitando UI dessincronizada do backend sem precisar de um refetch completo.
- `mesclarNotificacao` (lógica pura, testável sem DOM) evita duplicar notificação por id ao mesclar o resultado do fetch inicial com eventos STOMP — testado (`notificacoes-logic.test.ts`).
- Reuso integral do padrão de ticket de curta duração + backoff de reconexão já validado em `useBoardRealtime` (TASK-07.2), sem reinventar autenticação do handshake STOMP.

## Segurança

- **Autenticação/autorização:** subscrição do tópico `/topic/notificacoes/{usuarioId}` é autorizada pelo backend (`ChannelInterceptor`, já validado em TASK-05.1/05.2) — `usuarioId` usado no client vem de `usuario.id` resolvido server-side em `app/(shell)/layout.tsx` (não é input do usuário), sem forma de o browser subscrever o tópico de outro usuário.
- **XSS:** `mensagem` da notificação é renderizada via JSX (`{n.mensagem}`), sem `dangerouslySetInnerHTML` — sem risco de injeção.
- **Exposição de token:** o access token real nunca chega ao browser — o ticket STOMP é obtido via proxy Next.js (`POST /api/ws-ticket`, já revisado em TASK-07.2), mesmo padrão reaplicado aqui sem alteração.
- **IDOR em "marcar como lida":** `POST /api/notificacoes/{id}/lida` não valida ownership no frontend — delegado ao backend (`404` documentado em `contracts/dashboard-notificacoes.md` para notificação de outro usuário). Consistente com o padrão do restante do projeto (backend é a fonte da verdade).
- Nenhum secret hardcoded, nenhuma dependência nova introduzida por esta task (reuso de `@stomp/stompjs`/`sockjs-client` já presentes desde TASK-07.2).

Nenhum finding 🔴/🟡 de segurança.

## Conformidade com TechSpec

- Contrato (`docs/techspec/kanban-tarefas/contracts/dashboard-notificacoes.md`) respeitado: `GET /api/notificacoes`, `POST /api/notificacoes/{id}/lida` e o canal `/topic/notificacoes/{usuarioId}` usados exatamente como documentado.
- `lib/notificacoes.ts` espelha fielmente o shape do contrato (`id`, `tarefaId`, `tipo`, `mensagem`, `lida`, `criadoEm`).
- Desvio não bloqueante: query param `apenasNaoLidas=true` do contrato citado no fetch do client nunca chega ao backend por causa do proxy (I1) — mitigado hoje pelo comportamento atual do backend, mas é uma inconsistência de implementação vs. intenção documentada no próprio código.

## Resultado

**APROVADO COM RESSALVAS** — 0 críticos, 1 importante (I1), 2 sugestões não bloqueantes.
