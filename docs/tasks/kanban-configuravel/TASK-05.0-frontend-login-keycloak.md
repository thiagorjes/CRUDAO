# TASK-05.0 — Frontend: Login via Keycloak (OIDC Authorization Code) [M]

**Epic:** EPIC-05 — Frontend Next.js | **User Story:** US-05.1 — Interfaces do sistema
**Sistema:** CRUDAO | **RF:** RF-014 | **Dependências:** TASK-00.2, TASK-04.1

---

## Contexto

Lacuna identificada durante o `/implement` da TASK-05.1: nenhuma task cobria o fluxo de login do frontend, mas todos os endpoints do board exigem Bearer JWT desde a TASK-04.1 (client `crudao-app` é confidential, com secret — `publicClient: false` no realm). Sem esta task, o board não consegue autenticar contra a API. Criada e implementada em sequência, antes da TASK-05.1, com autorização do usuário.

## O que deve ser feito

- [ ] Rota de login que redireciona ao endpoint `/authorize` do Keycloak (Authorization Code, sem PKCE — client confidential)
- [ ] Callback que troca o `code` pelos tokens (usando o client secret, server-side) e grava sessão em cookie `httpOnly`
- [ ] Logout que limpa a sessão e redireciona ao `end_session_endpoint` do Keycloak
- [ ] `middleware.ts` protegendo todas as rotas de página, redirecionando não-autenticados para login
- [ ] Proxy autenticado (`/api/proxy/[...path]`) que anexa o `Authorization: Bearer` a partir da sessão e repassa ao backend — mantém o token fora do JS do browser
- [ ] Refresh automático do access_token expirado (via `refresh_token`) no proxy, com fallback para novo login se o refresh falhar

## Guia técnico

- Referência: `docs/contracts/CRUDAO-keycloak-contract.md`, ADR-003, `security.md`
- **Duas URLs do Keycloak** (mesma questão enfrentada na validação REST da TASK-03.1): a URL usada para redirecionar o *browser* (`/authorize`, `/logout`) precisa ser alcançável pelo host (`http://localhost:8081`); a URL usada para troca de tokens *server-side* (dentro do container) precisa ser a de rede interna (`http://keycloak:8080`) — variáveis separadas (`KEYCLOAK_ISSUER_URI` server-side, `KEYCLOAK_ISSUER_URI_PUBLIC` browser-facing), seguindo o mesmo padrão de override já usado pelo backend em `docker-compose.yml`
- Endpoint WebSocket (`/ws/**`) é `permitAll()` no backend (RF-005) — a conexão STOMP do browser não precisa do token, só as chamadas REST

## Critérios de aceite

- Acessar o frontend sem sessão redireciona para o login do Keycloak; após autenticar, retorna à página original
- Chamadas à API feitas pelo board (via proxy) chegam autenticadas ao backend
- Logout encerra a sessão local e no Keycloak

---

_Origem: lacuna identificada durante `/implement TASK-05.1` — 2026-08-22_
