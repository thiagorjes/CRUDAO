# Contrato MOCK — CRUDAO ↔ Keycloak
_Versão: 0.1 | Status: **PENDENTE DE VALIDAÇÃO** | Data: 2026-08-22_

> ⚠️ **Este é um contrato mock gerado automaticamente pelo /techspec.**
> Deve ser substituído pelo contrato real antes da integração em produção.
> Task de substituição: **TASK-KANBAN-KEYCLOAK-01** (a detalhar em /tasks — "Provisionar instância real de Keycloak e validar contrato OIDC")

---

## Identificação

| Campo | Valor |
|-------|-------|
| Interface | Autenticação OIDC (Authorization Code Flow) |
| Direção | CRUDAO (frontend Next.js + backend Spring Security) → Keycloak |
| Protocolo | OIDC/OAuth2 sobre HTTPS |
| Responsável (estimado) | Time CRUDAO (instância própria via Docker) |
| Versão mock | 0.1 |

---

## Descrição (estimada)

O frontend redireciona o usuário para o Keycloak (`quay.io/keycloak/keycloak`, executável via Docker) para login. Após autenticação, o Keycloak retorna um `authorization code`, trocado pelo backend por um `access_token`/`id_token` (JWT). O backend valida o token e extrai claims (`sub`, `email`, `preferred_username`, `realm_access.roles`) para associar ao usuário interno e seu mapeamento de papéis (ADR-003).

---

## Contrato de Dados (ESTIMADO — não validado com o sistema Keycloak)

### Entrada estimada

```
GET /realms/{realm}/protocol/openid-connect/auth
  ?client_id={client_id}&response_type=code&redirect_uri={callback_url}&scope=openid profile email

POST /realms/{realm}/protocol/openid-connect/token
  grant_type=authorization_code&code={code}&client_id={client_id}&client_secret={secret}
```

### Saída estimada

```json
{
  "access_token": "eyJ...",
  "id_token": "eyJ...",
  "refresh_token": "eyJ...",
  "expires_in": 300,
  "token_type": "Bearer"
}
```

Claims esperadas no `id_token`:
```json
{
  "sub": "uuid-do-usuario",
  "email": "usuario@exemplo.com",
  "preferred_username": "usuario",
  "realm_access": { "roles": ["default-roles-crudao"] }
}
```

---

## Erros conhecidos (estimados)

| Código | Significado estimado |
|--------|---------------------|
| 401 | Credenciais inválidas ou token expirado |
| 400 invalid_grant | Código de autorização inválido/expirado |
| 503 | Keycloak indisponível — sistema deve usar fallback de autenticação própria (RF-014 é Should Have) |

---

## Checklist de validação (a fazer antes de remover este aviso)

- [ ] Instância real de Keycloak provisionada (via Docker, conforme confirmado com o usuário)
- [ ] Realm e client configurados para o CRUDAO
- [ ] Schema de claims confirmado (roles reais mapeadas)
- [ ] Estratégia de fallback sem Keycloak testada
- [ ] Arquivo renomeado para `docs/contracts/CRUDAO-keycloak-contract.md`
- [ ] Status atualizado de `PENDENTE DE VALIDAÇÃO` para `ok`

---

## Histórico

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 0.1 | 2026-08-22 | /techspec (gerado) | Mock inicial — PENDENTE DE VALIDAÇÃO |
