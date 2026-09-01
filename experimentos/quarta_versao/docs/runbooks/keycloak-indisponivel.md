# Runbook (stub) — Keycloak indisponível

_Versão: 0.1 (stub) | TASK-08.2 | Data: 2026-09-01_

> Pré-requisito de go-live citado na TechSpec. Stub operacional: cobre sintoma,
> verificação e escalonamento. Detalhar antes da entrega em produção.

## Contexto

ADR-006: **não há fallback de autenticação local**. Com o Keycloak fora do ar,
nenhum login novo é possível e a validação de tokens (introspection) falha —
requisições autenticadas passam a receber `401`. Sessões já emitidas continuam
válidas até o token expirar.

## Sintoma

- Usuários não conseguem logar; redirecionamento OIDC falha ou expira.
- APIs autenticadas retornam `401`/`503` em massa.
- `GET /actuator/health` do backend com componente `keycloak` em `DOWN`
  (o grupo `readiness` inclui `keycloak` → pod sai de rotação).
- Logs do backend: falhas de conexão ao `issuer-uri` / `introspection-uri`.

## Verificação

1. Health do backend:
   `curl -s http://<backend>:8081/actuator/health | jq '.components.keycloak'`
2. Keycloak direto:
   `curl -sf http://<keycloak>:8080/realms/kanban-dev/.well-known/openid-configuration`
3. Container/serviço:
   `docker compose -p crudao ps keycloak` — estado e healthcheck.
   `docker compose -p crudao logs --tail=200 keycloak`
4. Banco do Keycloak acessível (se DB externo): conectividade e espaço em disco.
5. Rede/DNS entre backend e Keycloak (mesma rede Docker; `issuer-uri` resolvível).

## Ação imediata

- Se container parado/unhealthy: `docker compose -p crudao restart keycloak` e
  aguardar o healthcheck ficar `healthy`.
- Se erro de configuração recente (realm/client): reverter a última mudança de
  configuração do realm.
- Confirmar recuperação pelos passos 1–2 da Verificação; o `readiness` do backend
  volta a `UP` sozinho quando o `keycloak` health normaliza.

## Escalonamento

| Nível | Quando | Quem |
|---|---|---|
| N1 | Restart não resolve em 15 min | Plantão de infraestrutura |
| N2 | Keycloak sobe mas login/introspection seguem falhando | Time de identidade/SSO |
| N3 | Indisponibilidade > 1h ou perda de dados do realm | Gestor de incidentes + restauração de backup do Keycloak |

## Pendências deste stub

- Procedimento de restauração de backup do realm.
- Limiares de alerta e destino das notificações.
- RTO/RPO acordados com o negócio.
