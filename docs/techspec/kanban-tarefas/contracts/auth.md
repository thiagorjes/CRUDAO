# Contrato — Autenticação

_RF-014 | Sem fallback local ([ADR-006](../../../decisions/ADR-006-sem-fallback-auth-keycloak.md))_

## Fluxo

**Atualizado em TASK-07.1 (ver `memory/state.md`) — substitui a versão original abaixo.** O
Next.js atua como client OIDC próprio (padrão BFF), não o backend: faz o Authorization Code Flow
com PKCE diretamente contra o Keycloak, guarda os tokens em cookie httpOnly cifrado (nunca
expostos ao JS do browser) e repassa `Authorization: Bearer` só em chamadas server-side ao backend.
`keycloakSub` do token continua sendo usado pelo backend (`AtivoUsuarioFilter`) para
resolver/criar o `Usuario` local no primeiro login (provisioning just-in-time) — isso não muda.

O `oauth2Login`/sessão do Spring Security (`SecurityConfig.browserFilterChain`,
`OidcLoginSuccessHandler`) descritos abaixo ficam sem uso real neste fluxo — mantidos no backend
sem remoção nesta task, mas não são o caminho que o frontend percorre.

### GET /oauth2/authorization/keycloak (Spring, não utilizado pelo frontend)

Inicia o fluxo de login — redireciona ao Keycloak. Sem request body. O frontend usa em vez disso
`GET /api/auth/login` (Next.js, Route Handler) para iniciar seu próprio Authorization Code Flow.

### GET /login/oauth2/code/keycloak (callback — implementado pelo Next.js, não pelo Spring)

O `redirectUris` do client `kanban-frontend` (`keycloak/realm-export.json`) aponta para este
caminho no domínio do **frontend** (`http://localhost:3000/...`), não do backend — por isso é uma
Route Handler do Next.js (`frontend/app/login/oauth2/code/keycloak/route.ts`) que troca o código
por tokens (com PKCE), verifica assinatura/`iss`/`aud`/`nonce` do `id_token` via JWKS do Keycloak,
grava a sessão cifrada e redireciona ao frontend (`/projetos`) em caso de sucesso. O endpoint de
mesmo nome descrito no Spring Security (gerenciado automaticamente pelo framework) não é acionado
nesse fluxo.

## GET /api/me

Retorna o usuário autenticado e seus vínculos de projeto/papel (para o frontend montar a UI condicionalmente — RNF-003 exige que isso seja só UX, backend revalida sempre).

**Response 200:**
```json
{
  "id": "uuid",
  "nome": "string",
  "email": "string",
  "projetos": [{ "projetoId": "uuid", "papeis": ["dev"] }]
}
```

## POST /api/ws-ticket

_Adicionado em TASK-07.2 — ver `memory/state.md`._ Emite um ticket opaco de curta duração para
autenticar o handshake STOMP/SockJS (`GET /ws`) a partir do **browser**: a WebSocket API nativa não
permite enviar `Authorization: Bearer` no handshake, e o cookie de sessão do Next.js (BFF,
TASK-07.1) não é entendido pelo backend (origens diferentes, resource server só aceita Bearer). O
Next.js chama este endpoint server-side (com o Bearer real, nunca exposto) e repassa ao browser só
o ticket devolvido — o access token real nunca sai do servidor.

Autenticado normalmente via Bearer (resource server de `/api/**`). Sem request body.

**Response 200:**
```json
{ "ticket": "uuid", "expiraEm": "2026-08-26T12:00:00Z" }
```

O browser usa o ticket como `?ticket=<uuid>` na URL do SockJS (`GET /ws?ticket=...`), validado por
`WsTicketAuthenticationFilter` antes do resource server Bearer. Uso único (é marcado como usado na
primeira validação, mesmo se expirado) e TTL de 20s — cada tentativa de conexão/reconexão precisa
de um ticket novo. `GET /ws/info` (negociação de transporte do SockJS) fica fora da autenticação
por não expor dado sensível.

## POST /api/auth/logout

Encerra a sessão local e dispara RP-Initiated Logout no Keycloak (back-channel logout), invalidando o token no IdP — não apenas descartando a sessão local (achado do Comitê de Análise — Security).

**Response 204.**

## Erros

| Código | Situação |
|---|---|
| 401 | Token ausente/inválido/expirado — Keycloak indisponível também resulta em falha de login (sem fallback, ADR-006) |
| 401 | Token válido mas `Usuario.ativo=false` — toda checagem de autorização no backend valida `ativo`, não apenas o vínculo `UsuarioProjetoPapel` (achado do Comitê de Análise — Security) |
| 403 | Usuário autenticado mas sem nenhum vínculo de projeto (tela vazia, não erro bloqueante) |

RF atendido: RF-014.
