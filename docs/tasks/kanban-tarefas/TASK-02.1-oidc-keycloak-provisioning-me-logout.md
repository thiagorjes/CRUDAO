# TASK-02.1 — Integração OIDC Keycloak + provisioning JIT + /api/me + logout

**Tamanho:** [G] 1-2 dias
**Sistema:** CRUDAO
**RF de origem:** RF-014
**Dependências:** TASK-01.2
**Paralelismo:** nenhum

## Contexto

Habilita login via SSO — pré-requisito de qualquer ação autenticada no sistema. Sem fallback de autenticação local ([ADR-006](../../decisions/ADR-006-sem-fallback-auth-keycloak.md)).

## O que deve ser feito

- [ ] Configurar Spring Security OAuth2 Client para Authorization Code Flow com Keycloak.
- [ ] Implementar provisioning just-in-time de `Usuario` a partir do `sub`/claims do token no primeiro login.
- [ ] Implementar `GET /api/me` retornando usuário + vínculos projeto/papel.
- [ ] Implementar `POST /api/auth/logout` com RP-Initiated Logout (back-channel) no Keycloak.
- [ ] Retornar `401` se `Usuario.ativo=false` mesmo com token válido (achado do Comitê — Security).
- [ ] Health-check dedicado de dependência Keycloak (`/actuator/health/keycloak` ou equivalente).

## Guia técnico

- `backend/src/main/java/.../security/` — configuração OAuth2 Client.
- `backend/src/main/java/.../auth/` — provisioning JIT, `/api/me`, `/api/auth/logout`.
- Contrato completo: `docs/techspec/kanban-tarefas/contracts/auth.md`.
- ADR-006: `docs/decisions/ADR-006-sem-fallback-auth-keycloak.md`.

Response de `GET /api/me`:
```json
{
  "id": "uuid",
  "nome": "string",
  "email": "string",
  "projetos": [{ "projetoId": "uuid", "papeis": ["dev"] }]
}
```

## Critérios de aceite

- Login via Keycloak (ambiente dev de TASK-01.1) redireciona corretamente e cria `Usuario` local no primeiro acesso.
- `GET /api/me` retorna estrutura do contrato.
- `POST /api/auth/logout` invalida sessão local e token no Keycloak (verificável por tentativa de reuso do token).
- Usuário com `ativo=false` recebe `401` em qualquer endpoint autenticado.
- Health-check reflete indisponibilidade do Keycloak quando o container é parado.
- `401` também quando token ausente/inválido/expirado ou Keycloak indisponível (sem fallback, ADR-006).
