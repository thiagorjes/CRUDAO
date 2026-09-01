# Code Review — TASK-07.7 (Notificações UI + autenticação WebSocket por ticket)

_Revisor: /code-review v1.0 — 2026-09-01 (re-review pós-correção de C1/I1)_
_Sistema: CRUDAO (frontend + backend) · Feature: kanban-tarefas · RF-005_

## Contexto

Primeiro review (contexto fresco) → **REPROVADO**: C1 (tempo real inoperante — token STOMP lido de `document.cookie` inexistente), I1 (backend só `.withSockJS()`, incompatível com `new WebSocket()` cru), I2 (dedup framing STOMP), I3 (contrato desatualizado). Decisão do usuário: corrigir agora, opção "ticket de curta duração" (cross-cutting board + notificações). Este re-review cobre a correção.

## Gate de testes

- `WsTicketServiceTest` — 5 testes ✅ (`mvn -o test -Dtest=WsTicketServiceTest`)
- Backend `mvn -o compile` + `mvn -o test-compile` ✅
- Frontend `tsc --noEmit` ✅ · `vitest` ✅ (5, sem cobertura de notificações/STOMP) · `next build` ✅ (rota `/api/ws-ticket` registrada)
- **Não re-executado nesta sessão:** suíte `-P integration-tests` (164) contra o stack Docker. Nenhum teste referencia SockJS/StompClient/ticket, então o risco estático é baixo — mas a remoção do SockJS e o filtro de ticket **não foram exercitados em runtime**.
- **Handshake WebSocket ponta a ponta NÃO foi validado em runtime** (sem stack Docker nesta sessão).

## Critérios de Aceite

| # | Critério | Verificado? | Evidência |
|---|----------|-------------|-----------|
| 1 | Notificação em tempo real p/ observador de tarefa alterada | ⚠️ | Caminho agora coeso: `NotificacoesSino.tsx:56-66` obtém ticket → `notificacoes-stomp.ts:52-56` conecta em `/ws?ticket=` → `WsTicketAuthenticationFilter` valida e popula o `Principal` (name=e-mail) → `BoardChannelInterceptor.java:67-77` autoriza `/topic/notificacoes/{id}`. Estrutura correta; **pendente smoke em runtime**. |
| 2 | Marcar como lida reflete backend + UI | ✅ | `NotificacoesSino.tsx:88-98` (otimista + reversão); proxy `app/api/notificacoes/[id]/marcar-como-lida/route.ts`; backend `NotificacaoController.java:64-74` valida ownership. |
| 3 | Subscrição em `/topic/notificacoes/{usuarioId}`; authz no backend | ✅ (código) | `notificacoes-stomp.ts:105`; `BoardChannelInterceptor.java:112-125` (`usuario.getId().equals(usuarioIdDestino)`). Runtime pendente (idem critério 1). |

## 🔴 Crítico

Nenhum residual. C1 e I1 estruturalmente resolvidos.

## 🟡 Importante

#### I1-resid — `SecurityConfig` e `StompConfig` ainda citam SockJS / `/ws/info` — ✅ RESOLVIDO (2026-09-01)
`SecurityConfig`: removido `/ws/info` do `permitAll` e reescritos os javadocs/comentários do bloco `/ws/**` (agora descrevem o ticket `?ticket=`, sem SockJS). `StompConfig`: javadoc de fluxo atualizado ("WebSocket puro, autenticado por ticket, sem SockJS").

## 🔵 Sugestão

#### S1 — Ticket trafega na query string
Arquivo: `notificacoes-stomp.ts:53`, `stomp.ts:76-78`.
Problema: `?ticket=` aparece em access logs, histórico do browser e `Referer`. Mitigado por TTL de 30s e escopo único (handshake WS). É o tradeoff padrão de auth em WebSocket nativo (não há header no upgrade). Manter TTL curto; não logar a query no backend.
Guideline: `security.md` — não coberto explicitamente; recomendo registrar como decisão aceita.

#### S2 — Segredo default do ticket sem fail-fast — ✅ RESOLVIDO (2026-09-01)
`WsTicketService` agora recebe `@Value("${kanban.ws-ticket.secret:}")` (sem default público) e o construtor **falha o boot** se o segredo for nulo/branco ou < 16 caracteres. `application.yml` resolve `${WS_TICKET_SECRET:}` (vazio ⇒ boot falha em produção sem a env). `application-dev.yml` / `-test.yml` / `-it.yml` declaram cada um o seu segredo. Teste novo `construtor_falha_quando_segredo_ausente_ou_curto` (`WsTicketServiceTest` → 6 testes ✅).

#### S3 — `setAllowedOrigins("*")` no endpoint `/ws`
Arquivo: `websocket/StompConfig.java`.
Problema: pré-existente. Qualquer origem pode abrir o handshake. Mitigado pelo ticket (uma página maliciosa não consegue emitir ticket sem a sessão do BFF, same-origin), mas convém restringir a origens conhecidas por ambiente.
Guideline: `security.md` — CSWSH / OWASP A05.

#### S4 — I2 (dedup do framing STOMP) não endereçado
`lib/stomp.ts` e `lib/notificacoes-stomp.ts` duplicam `serializar`/`deserializar`. Aceito como ressalva pós-merge (extrair `lib/stomp-framing.ts`).

#### S5 — `shouldNotFilter` casa prefixo amplo
Arquivo: `websocket/WsTicketAuthenticationFilter.java:41`.
Problema: `getRequestURI().startsWith("/ws")` casaria `/wsqualquercoisa`. Hoje não existe outra rota `/ws*` além de `/ws`, e `/api/ws-ticket` não é afetado (começa com `/api`). Cosmético; tightening para `equals("/ws")` ou `startsWith("/ws?")`/`"/ws/"` é mais preciso.

## Qualidade de Código

- **Corrigido durante o re-review** (2 itens, baixo risco, verificados com `tsc`/`vitest`/`build`):
  - `StompManager` (board) ganhou flag `encerrado` — `desconectar()` agora impede a reconexão automática pós-unmount (o `ws.onclose` disparava `_reconectar()` → `setTimeout` → `conectar()` → nova chamada de `getTicket()` após o componente sair). Bug pré-existente do board, agravado pela introdução do fetch de ticket nesse caminho. `NotificacoesStomp` já tinha a guarda equivalente.
  - `lib/api.ts` `apiProxyFetch` passou a definir `Content-Type: application/json` quando há corpo e nenhum header próprio. Sem isso, ao fazer `lib/api/proxy.ts` delegar para a implementação canônica, **todos** os route handlers de mutação (`admin/*` POST/PUT, `tarefas/*` PUT/POST/mover/impedimento/observadores) enviariam `text/plain` e o Spring responderia 415 — regressão que a delegação teria introduzido.
- `WsTicketService`: HMAC com comparação em tempo constante, base64url sem padding, parsing defensivo (todos os ramos inválidos → `Optional.empty()`), stateless (sem store → multi-pod safe, constituição §5).
- Nomenclatura pt_BR consistente; `getTicket: () => Promise<string>` como porta injetável nos dois clientes STOMP — testável e desacoplado.
- `lib/api/proxy.ts` reduzido a re-export da implementação canônica — elimina a divergência de comportamento entre `@/lib/api` e `@/lib/api/proxy`.

## Segurança

- **Positivo:** access token do Keycloak nunca chega ao JS (o cookie `kanban_session` permanece httpOnly/cifrado; o ticket carrega só o e-mail e expira em 30s). Fail-closed: ticket ausente → 401 no handshake; ticket inválido/expirado/de outro segredo → 401 (coberto por `WsTicketServiceTest`). Autorização de subscrição continua 100% no backend (`BoardChannelInterceptor`). Ownership do `marcar-como-lida` no backend.
- **Atenção:** S2 (segredo default) é o item de maior peso — resolver antes de qualquer deploy fora de dev.
- **Pré-existente reforçado:** o filtro de ticket agora é o único mecanismo de auth do handshake `/ws`; garantir que `WS_TICKET_SECRET` seja igual em todos os pods (o ticket é validado por qualquer pod, mas só se compartilharem o segredo).
- Sem secrets hardcoded no frontend; sem PII adicional em log (`console.error` com prefixo).
- `next build` sem novos avisos; nenhuma dependência adicionada.

## Conformidade com TechSpec

- **C1/I1:** o design de ticket de curta duração é exatamente o descrito (aspiracionalmente) em `lib/env.ts:22-27` e no javadoc de `SecurityConfig` — agora implementado de fato (`WsTicketService`/`WsTicketController`/`WsTicketAuthenticationFilter`).
- **I3:** contrato `dashboard-notificacoes.md` reconciliado (`GET /api/notificacoes` sem query, `tarefaTitulo`, `PUT /{id}/marcar-como-lida`, payload STOMP como gatilho).
- **ADR-004 / Canvas S (STOMP):** subscrição validada no backend — mantido. Reconexão com backoff 1s→30s nos dois clientes.
- **Constituição §5 (multi-pod sem estado local):** ticket stateless (HMAC) respeita o princípio; **não** foi introduzido store em memória.
- **Canvas N (tipos 1:1 com DTO):** `Notificacao` espelha `NotificacaoResponse`.
- Removção do SockJS: desvio consciente da TechSpec (que citava "fallback SockJS"). Justificado — nenhum cliente usa e é incompatível com o cliente WS cru. Registrar em Canvas S (feito) e, idealmente, nota na TechSpec.

## Resultado

**APROVADO COM RESSALVAS**

- 0 críticos residuais · 0 importantes residuais (I1-resid resolvido) · 4 sugestões pendentes (S1, S3, S4, S5 — pós-merge).
- **Ressalva bloqueante para fechar o RF-005 / EPIC 07:** validar o handshake WebSocket ponta a ponta em runtime (login → board/notificações → SUBSCRIBE aceito → evento entregue) via smoke no `docker compose` ou `run-integration-tests.ps1`. O caminho de código está coeso e revisado, mas nunca foi exercitado com um broker real.
- **S2 e I1-resid resolvidos** neste ciclo (2026-09-01).

### Nota sobre a suíte de testes

`mvn -o test` (sem Docker) acusa 40 erros — **todos** em testes `@ActiveProfiles("it")` que falham no load de contexto por `PSQLException: connection failed` (não há Postgres/compose nesta sessão). É condição pré-existente e documentada (ITs rodam só via `run-integration-tests.ps1`). Testes unitários puros passam; `WsTicketServiceTest` 6/6 ✅; `mvn -o compile`/`test-compile` ✅.

## ✅ Pontos Positivos

- A correção tratou a **causa raiz sistêmica**, não só o sintoma nas notificações: `lib/api/proxy.ts` consertado beneficia todos os route handlers de proxy; o ticket serve board e notificações com a mesma porta `getTicket`.
- `WsTicketService` stateless com teste unitário cobrindo adulteração de payload, de assinatura, segredo trocado e entradas malformadas — a parte de maior risco tem rede de segurança.
- Dois bugs latentes do board (reconexão pós-unmount; ausência de `Content-Type` nos proxies) foram encontrados e corrigidos no caminho, evitando regressão.
