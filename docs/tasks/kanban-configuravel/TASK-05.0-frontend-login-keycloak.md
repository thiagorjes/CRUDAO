# TASK-05.0 — Frontend: Login via Keycloak (OIDC Authorization Code) [M]

**Epic:** EPIC-05 — Frontend Next.js | **User Story:** US-05.1 — Interfaces do sistema
**Sistema:** CRUDAO | **RF:** RF-014 | **Dependências:** TASK-00.2, TASK-04.1
**Status: Concluída — 2026-08-22**

---

## Contexto

Lacuna identificada durante o `/implement` da TASK-05.1: nenhuma task cobria o fluxo de login do frontend, mas todos os endpoints do board exigem Bearer JWT desde a TASK-04.1 (client `crudao-app` é confidential, com secret — `publicClient: false` no realm). Sem esta task, o board não consegue autenticar contra a API. Criada e implementada em sequência, antes da TASK-05.1, com autorização do usuário.

## O que deve ser feito

- [x] Rota de login que redireciona ao endpoint `/authorize` do Keycloak (Authorization Code, sem PKCE — client confidential)
- [x] Callback que troca o `code` pelos tokens (usando o client secret, server-side) e grava sessão em cookie `httpOnly`
- [x] Logout que limpa a sessão e redireciona ao `end_session_endpoint` do Keycloak
- [x] `src/proxy.ts` (convenção Next 16, substitui `middleware.ts`) protegendo todas as rotas de página, redirecionando não-autenticados para login
- [x] Proxy autenticado (`/api/proxy/[...path]`) que anexa o `Authorization: Bearer` a partir da sessão e repassa ao backend — mantém o token fora do JS do browser
- [x] Refresh automático do access_token expirado (via `refresh_token`) no proxy, com fallback (401) para novo login se o refresh falhar

## Guia técnico

- Referência: `docs/contracts/CRUDAO-keycloak-contract.md`, ADR-003, `security.md`
- **Duas URLs do Keycloak** (mesma questão enfrentada na validação REST da TASK-03.1): a URL usada para redirecionar o *browser* (`/authorize`, `/logout`) precisa ser alcançável pelo host (`http://localhost:8081`); a URL usada para troca de tokens *server-side* (dentro do container) precisa ser a de rede interna (`http://keycloak:8080`) — variáveis separadas (`KEYCLOAK_ISSUER_URI` server-side, `KEYCLOAK_ISSUER_URI_PUBLIC` browser-facing), seguindo o mesmo padrão de override já usado pelo backend em `docker-compose.yml`
- Endpoint WebSocket (`/ws/**`) é `permitAll()` no backend (RF-005) — a conexão STOMP do browser não precisa do token, só as chamadas REST

## Critérios de aceite

- Acessar o frontend sem sessão redireciona para o login do Keycloak; após autenticar, retorna à página original
- Chamadas à API feitas pelo board (via proxy) chegam autenticadas ao backend
- Logout encerra a sessão local e no Keycloak

## Nota técnica

Durante a validação end-to-end real (não só testes unitários), um achado adicional bloqueou o critério de aceite "chamadas via proxy chegam autenticadas": no modo `start-dev` do Keycloak (sem hostname fixo), o `iss` estampado no token de um login via Authorization Code segue o host usado no `/authorize` (público, `http://localhost:8081` — só o browser alcança), não o host usado na troca do `code` por token (interno, `http://keycloak:8080` — só o backend alcança). O backend rejeitava o token com 401 por mismatch de issuer. Corrigido separando, no `SecurityConfig` do backend, a busca de chaves JWKS (URL interna) da validação do claim `iss` (URL pública) — `crudao.keycloak.issuer-uri-interno`/`issuer-uri-publico` em `application.yml`.

## Code review (agent QA)

1 finding 🔴 (open redirect via `returnTo` não validado) e 5 🟡 corrigidos: validação de `returnTo` como path relativo (`caminhoRelativoSeguro`, revalidado tanto na escrita quanto no uso), fallback 401 explícito quando o refresh do token falha (antes propagava como 500), dedupe de renovações concorrentes do mesmo `refresh_token` (`garantirSessaoValida`), `try/catch` no parse do cookie de state, guardrail de log para `COOKIE_SECURE` ausente em produção. Guardrails G-AUTH-01 a G-AUTH-04 registrados na dimensão Safeguards do canvas.

---

_Origem: lacuna identificada durante `/implement TASK-05.1` — 2026-08-22_
