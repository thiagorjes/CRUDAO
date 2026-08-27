---
id: ADR-003
type: ADR
status: accepted
date: 2026-08-22
supersedes: —
superseded-by: —
---

# ADR-003 — RBAC híbrido: Keycloak para autenticação, permissões modeladas na aplicação

## Decisão

Keycloak (OIDC) autentica o usuário e define papéis de alto nível; a aplicação mantém seu próprio modelo de papéis/permissões granulares (RF-013), mapeando/associando os papéis do Keycloak às permissões internas configuráveis pelo admin.

## Motivação

O PRD exige papéis configuráveis dinamicamente pelo admin após implantação (RF-013), o que não é natural de gerenciar apenas via claims/roles estáticos do Keycloak. Delegar autenticação ao Keycloak e granularidade de permissão à aplicação equilibra SSO com flexibilidade de negócio.

**Problema que resolve:**
Permitir que o admin crie/edite papéis e permissões em tempo de operação sem depender de reconfiguração no Keycloak.

**Restrições consideradas:**
- RF-014 (SSO Keycloak) é Should Have — sistema deve funcionar com autenticação própria caso Keycloak indisponível (fallback a definir em techspec).
- RN-006: papel admin não pode ser criado/editado/excluído por papel delegado.

## Consequências

**Positivas:**
- Flexibilidade total de papéis/permissões sem depender de deploy no Keycloak.
- SSO centralizado para autenticação.

**Negativas / trade-offs:**
- Duplicação conceitual entre "papel" no Keycloak e "papel" na aplicação exige mapeamento explícito documentado em techspec.

**Downstream afetado:**
- TechSpec: modelo de dados de papéis/permissões e estratégia de fallback sem Keycloak.

## Alternativas Consideradas

### Alternativa 1 — Papéis e permissões 100% no Keycloak
**Descartada porque:** exigiria acesso administrativo ao Keycloak para toda alteração de permissão, o que contraria a exigência de autoatendimento pelo admin da aplicação.

### Alternativa 2 — Autenticação local própria (sem Keycloak)
**Descartada porque:** RF-014 pede explicitamente SSO via Keycloak como Should Have.
