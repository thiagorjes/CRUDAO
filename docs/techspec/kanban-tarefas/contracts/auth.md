# Contrato — Autenticação

_RF-014 | Sem fallback local ([ADR-006](../../../decisions/ADR-006-sem-fallback-auth-keycloak.md))_

## Fluxo

OIDC Authorization Code Flow via Spring Security OAuth2 Client, redirecionando para Keycloak. Sessão do usuário estabelecida via JWT (Bearer) nas chamadas subsequentes à API; `keycloakSub` do token é usado para resolver/criar o `Usuario` local no primeiro login (provisioning just-in-time).

## GET /oauth2/authorization/keycloak

Inicia o fluxo de login — redireciona ao Keycloak. Sem request body.

## GET /login/oauth2/code/keycloak (callback)

Gerenciado pelo Spring Security — troca o código de autorização por tokens e cria a sessão. Redireciona ao frontend (`/projetos`) em caso de sucesso.

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
