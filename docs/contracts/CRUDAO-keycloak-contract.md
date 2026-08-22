# Contrato — CRUDAO ↔ Keycloak
_Versão: 1.0 | Status: **ok** | Data: 2026-08-22_

> Contrato validado contra instância real de Keycloak (Docker, dev-mode), conforme TASK-00.1.
> Estratégia de fallback sem Keycloak segue como questão em aberto (Q-002 da techspec) — a implementar na TASK-04.1.

---

## Identificação

| Campo | Valor |
|-------|-------|
| Interface | Autenticação OIDC (Authorization Code Flow) |
| Direção | CRUDAO (frontend Next.js + backend Spring Security) → Keycloak |
| Protocolo | OIDC/OAuth2 sobre HTTP (dev) / HTTPS (produção) |
| Responsável | Time CRUDAO (instância própria via Docker, `docker-compose.yml`) |
| Realm | `crudao` |
| Client | `crudao-app` (confidential, Authorization Code + Direct Access Grants habilitados) |
| Versão | 1.0 |

---

## Descrição

O frontend redireciona o usuário para o Keycloak para login. Após autenticação, o Keycloak retorna um `authorization code`, trocado pelo backend por um `access_token`/`id_token` (JWT). O backend valida o token e extrai claims (`sub`, `email`, `preferred_username`, `realm_access.roles` no `access_token`) para associar ao usuário interno e seu mapeamento de papéis (ADR-003).

Realm provisionado via import automático (`infra/keycloak/crudao-realm.json`), com papéis `admin`/`user` e usuários de teste (`admin.teste`/`admin123`, `user.teste`/`user123`).

---

## Contrato de Dados (validado)

### Entrada

```
GET /realms/crudao/protocol/openid-connect/auth
  ?client_id=crudao-app&response_type=code&redirect_uri={callback_url}&scope=openid profile email

POST /realms/crudao/protocol/openid-connect/token
  grant_type=authorization_code&code={code}&client_id=crudao-app&client_secret={secret}
```

### Saída (validado via password grant, 2026-08-22)

```json
{
  "access_token": "eyJ...",
  "id_token": "eyJ...",
  "refresh_token": "eyJ...",
  "expires_in": 300,
  "token_type": "Bearer"
}
```

Claims confirmadas no `id_token`:
```json
{
  "sub": "110c0a15-3ac6-466e-96bb-09adb4155d6c",
  "email": "admin.teste@crudao.local",
  "preferred_username": "admin.teste",
  "email_verified": true,
  "name": "Admin Teste"
}
```

Claims confirmadas no `access_token`:
```json
{
  "realm_access": { "roles": ["admin"] }
}
```

**Nota:** `realm_access.roles` está no `access_token`, não no `id_token` — ajustar a implementação da TASK-04.1 para ler o papel a partir do `access_token` (ou de um endpoint `userinfo`), não do `id_token`.

---

## Erros conhecidos

| Código | Significado |
|--------|---------------------|
| 401 | Credenciais inválidas ou token expirado |
| 400 invalid_grant | Código de autorização inválido/expirado |
| 503 / conexão recusada | Keycloak indisponível — sistema deve usar fallback de autenticação própria (RF-014 é Should Have) — fallback ainda a implementar na TASK-04.1 |

---

## Setup local

```
docker compose up -d keycloak
```

Realm `crudao` é importado automaticamente de `infra/keycloak/crudao-realm.json`. Console admin em `http://localhost:8081` (usuário `admin`/`admin`, apenas dev).

---

## Histórico

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 0.1 | 2026-08-22 | /techspec (gerado) | Mock inicial — PENDENTE DE VALIDAÇÃO |
| 1.0 | 2026-08-22 | TASK-00.1 (implementação) | Contrato validado contra instância real via Docker; claims confirmadas; nota sobre localização de `realm_access.roles` |
