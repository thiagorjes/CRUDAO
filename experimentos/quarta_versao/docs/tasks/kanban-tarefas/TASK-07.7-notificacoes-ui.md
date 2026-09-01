# TASK-07.7 — Notificações UI

**Tamanho:** [P] ≤4h
**Sistema:** CRUDAO
**RF de origem:** RF-005
**Dependências:** TASK-07.1, TASK-05.2
**Paralelismo:** [P] com TASK-07.4, TASK-07.5, TASK-07.6

## Contexto

Fecha o ciclo de visibilidade de impedimento — objetivo central do PRD.

## O que deve ser feito

- [x] Lista de notificações não lidas, conectada a `/topic/notificacoes/{usuarioId}`; o backend garante que o tópico só pode ser assinado pelo próprio usuário.
- [x] Ação de marcar como lida.

## Guia técnico

- `frontend/components/notificacoes/`

## Critérios de aceite

- Notificação aparece em tempo real quando o usuário é observador de uma tarefa alterada.
- Marcar como lida reflete no backend e na UI.

## Status: Concluída — 2026-09-01

> Nota: o marcador "Concluída — 2026-08-26" acima era de template; implementação real feita em 2026-09-01.

### Implementação (2026-09-01)

- `lib/types.ts` — tipo `Notificacao` (espelha `NotificacaoController.NotificacaoResponse`: `id, tarefaId, tarefaTitulo, tipo, lida, criadoEm`).
- `lib/api/notificacoes.ts` — `listarNaoLidas()` (GET `/api/notificacoes`), `marcarComoLida(id)` (PUT `/api/notificacoes/{id}/marcar-como-lida`).
- `app/api/notificacoes/route.ts` + `app/api/notificacoes/[id]/marcar-como-lida/route.ts` — route handlers proxy (auth via cookie de sessão em `apiProxyFetch`).
- `lib/notificacoes-stomp.ts` — cliente STOMP dedicado: subscreve `/topic/notificacoes/{usuarioId}`, token no frame CONNECT, reconexão com backoff exponencial (1s→30s). Payload é gatilho — cada MESSAGE dispara recarga da lista via REST.
- `components/notificacoes/NotificacoesSino.tsx` — sino na topbar com badge de contagem, painel de lista, "marcar como lida" com update otimista + reversão em falha, fecha ao clicar fora.
- `components/DashboardShell.tsx` — placeholder do sino substituído por `<NotificacoesSino usuarioId={me.id} />`.

### Divergência de contrato (seguido o backend real)

`docs/techspec/kanban-tarefas/contracts/dashboard-notificacoes.md` descreve `GET /api/notificacoes?apenasNaoLidas=true` com campo `mensagem` e `POST /api/notificacoes/{id}/lida`. O backend implementado (TASK-05.2) expõe `GET /api/notificacoes` (já retorna só não lidas, sem query param), campo `tarefaTitulo` (não `mensagem`), e `PUT /api/notificacoes/{id}/marcar-como-lida`. Frontend seguiu o backend real.

Verificação: `tsc --noEmit` ✅, `vitest` ✅ (5), `next build` ✅ (rotas `/api/notificacoes` e `/api/notificacoes/[id]/marcar-como-lida` registradas). E2E runtime não executado (sem stack Docker nesta sessão).

### Pós-code-review — correção de C1/C1-I1 (autenticação WebSocket) — 2026-09-01

O review (contexto fresco) reprovou: o tempo real não funcionava em runtime porque os clientes STOMP (board **e** notificações) liam o token do handshake de `document.cookie` procurando `session=` — mas o cookie é `kanban_session`, httpOnly e cifrado (o JS nunca vê o token). Além disso o backend expunha o endpoint só com `.withSockJS()`, incompatível com `new WebSocket()` cru. Correção (cross-cutting, decisão do usuário: opção "ticket de curta duração"):

**Backend:**
- `WsTicketService` — emite/valida ticket assinado (HMAC-SHA256, TTL 30s, e-mail no payload), stateless (multi-pod safe, constituição §5).
- `WsTicketController` — `POST /api/ws-ticket` (autenticado como qualquer `/api/**`) → `{ ticket, expiraEmSegundos }`.
- `WsTicketAuthenticationFilter` — em `/ws**` com `?ticket=`, valida e popula o `SecurityContext` (`Principal.name` = e-mail, que o `BoardChannelInterceptor` já usa). Registrado antes do `BearerTokenAuthenticationFilter`.
- `StompConfig` — removido SockJS; endpoint `/ws` puro (nenhum cliente usava SockJS e é incompatível com WS cru).
- `application.yml` — `kanban.ws-ticket.secret` (env `WS_TICKET_SECRET`, default dev).
- Teste: `WsTicketServiceTest` (5, roundtrip/adulteração/segredo trocado/malformado) — 100% verde (`mvn -o test`).

**Frontend:**
- `lib/api/proxy.ts` — corrigido: passa a reexportar `apiProxyFetch` de `lib/api.ts` (cookie `kanban_session` + `decifrarSessao` + `env.backendUrl()`). Isso conserta a autenticação de **todos** os route handlers que importavam `@/lib/api/proxy` (dashboard, tarefas, notificações).
- `app/api/ws-ticket/route.ts` + `lib/api/ws-ticket.ts` — obtém o ticket.
- `lib/stomp.ts` (board) e `lib/notificacoes-stomp.ts` — construtor agora recebe `getTicket: () => Promise<string>` e URL base; conectam em `ws://.../ws?ticket=...`; ticket novo a cada (re)conexão; removido header `authorization` do frame CONNECT.
- `board/page.tsx` e `NotificacoesSino.tsx` — removido o hack de `document.cookie`; passam `obterWsTicket`.

Também reconciliado o contrato `docs/techspec/kanban-tarefas/contracts/dashboard-notificacoes.md` (I3): `GET /api/notificacoes` sem query, campo `tarefaTitulo`, `PUT /{id}/marcar-como-lida`, payload STOMP como gatilho.

Verificação pós-correção: backend `mvn -o compile` + `test-compile` ✅, `WsTicketServiceTest` ✅; frontend `tsc` ✅, `vitest` ✅ (5), `next build` ✅ (rota `/api/ws-ticket` registrada). **Handshake WS ponta a ponta ainda não exercitado em runtime** (sem stack Docker nesta sessão) — validar com `run-integration-tests.ps1` / smoke manual.
I2 (dedup do framing STOMP entre `stomp.ts` e `notificacoes-stomp.ts`) aceito como ressalva pós-merge.

### Re-review (2026-09-01) — APROVADO COM RESSALVAS + fixes S2/I1-resid

Re-review em contexto fresco: **APROVADO COM RESSALVAS** (0 críticos, 0 importantes residuais, 4 sugestões pós-merge). Corrigidos no ciclo:
- `StompManager` (board): flag `encerrado` — `desconectar()` impede reconexão automática pós-unmount (que dispararia novo `getTicket()`).
- `lib/api.ts` `apiProxyFetch`: define `Content-Type: application/json` quando há corpo — sem isso a delegação de `lib/api/proxy.ts` causaria HTTP 415 em todos os route handlers de mutação (`admin/*`, `tarefas/*`).
- **S2:** `WsTicketService` sem default público; boot falha se `kanban.ws-ticket.secret` ausente/branco/<16 chars. Secret em `application-{dev,test,it}.yml`; `application.yml` = `${WS_TICKET_SECRET:}`; `docker-compose.yml` seta `WS_TICKET_SECRET` no backend. Teste `construtor_falha_quando_segredo_ausente_ou_curto` (`WsTicketServiceTest` → 6 ✅).
- **I1-resid:** `SecurityConfig` sem `permitAll("/ws/info")` + javadocs reescritos; `StompConfig` javadoc de fluxo sem SockJS.

Verificação: `mvn -o compile`/`test-compile` ✅, `WsTicketServiceTest` 6 ✅. `mvn -o test` sem Docker acusa 40 erros pré-existentes (ITs `@ActiveProfiles("it")` sem Postgres — rodam só via `run-integration-tests.ps1`). Frontend `tsc`/`vitest`/`next build` ✅.

**Única ressalva bloqueante restante:** smoke do handshake WebSocket ponta a ponta no compose antes de fechar o RF-005 / EPIC 07.
