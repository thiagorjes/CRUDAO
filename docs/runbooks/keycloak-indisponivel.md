# Runbook — Keycloak indisponível

_Stub operacional — TASK-08.2, pré-requisito de go-live citado na TechSpec (fora do escopo funcional)._

## Sintoma

- Login (`/login`) trava ou retorna erro no fluxo OIDC (Authorization Code + PKCE) do frontend.
- Chamadas ao backend (`/api/**`) passam a responder `401` em massa para usuários com sessão expirada — o resource server valida token via introspecção/JWKS do Keycloak a cada renovação.
- `GET /actuator/health` do backend permanece `UP` (Keycloak não é dependência do resource server em runtime para tokens ainda válidos) — a indisponibilidade só aparece no fluxo de login/renovação, não no health-check da aplicação.
- Logs do backend (`logs/kanban-backend.log`) com `ConnectException`/`timeout` nas chamadas ao endpoint OIDC do Keycloak.

## Verificação

1. Checar se o container/serviço do Keycloak está de pé: `docker compose ps keycloak` (ambiente dev) ou painel do orquestrador em produção.
2. Testar o endpoint de discovery diretamente: `curl -f <KEYCLOAK_URL>/realms/kanban-dev/.well-known/openid-configuration`.
3. Checar logs do próprio Keycloak (banco de dados do realm, memória, CPU).
4. Confirmar se é indisponibilidade total ou lentidão (timeout) — lentidão pode ser rede/DB do Keycloak, não o processo em si.

## Escalonamento

- **RN de produto (ADR-006):** não há fallback de autenticação local — usuários sem sessão válida ficam bloqueados até o Keycloak voltar. Não é um bug do backend, é uma decisão de arquitetura aceita.
- Sessões já autenticadas com token ainda válido continuam funcionando até a expiração/necessidade de renovação.
- Se a indisponibilidade persistir além do tempo aceitável combinado com o time (SLA a definir), escalar para o responsável pela infraestrutura do Keycloak (fora do escopo deste sistema — IdP é premissa externa, ver ADR-006).
- Comunicar aos usuários (canal interno) que o login está temporariamente indisponível, sem necessidade de abrir chamado no backend do Kanban.

## Fora de escopo deste stub

Runbook detalhado de recuperação do próprio Keycloak (backup/restore de realm, failover) é responsabilidade do time que opera o IdP — não coberto aqui.
