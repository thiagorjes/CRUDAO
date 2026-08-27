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

Keycloak (OIDC) autentica o usuário e define papéis de alto nível; a aplicação mantém seu próprio modelo de papéis e permissões granulares (RF-013), associando os papéis internos aos usuários e projetos.

## Motivação

O PRD exige papéis configuráveis dinamicamente pelo admin após implantação (RF-013), o que não é natural de gerenciar apenas via claims/roles estáticos do Keycloak. Delegar autenticação ao Keycloak e granularidade de permissão à aplicação equilibra SSO com flexibilidade de negócio.

**Problema que resolve:**

Permitir que o admin crie e edite papéis e permissões em tempo de operação sem depender de reconfiguração no Keycloak.

**Restrições consideradas:**

- RF-014 (SSO Keycloak) é Should Have.
- RN-006: papel `admin` não pode ser criado, editado ou excluído por papel delegado.

## Consequências

**Positivas:**

- Flexibilidade total de papéis e permissões sem depender de deploy no Keycloak.
- SSO centralizado para autenticação.

**Negativas / trade-offs:**

- Duplicação conceitual entre papéis no Keycloak e na aplicação exige mapeamento explícito na TechSpec.

**Downstream afetado:**

- TechSpec: modelo de dados de papéis/permissões e estratégia de autorização.

## Alternativas Consideradas

### Alternativa 1 — Papéis e permissões 100% no Keycloak

**Descartada porque:** exigiria acesso administrativo ao Keycloak para toda alteração de permissão, contrariando o autoatendimento pelo admin da aplicação.

### Alternativa 2 — Autenticação local própria sem Keycloak

**Descartada porque:** RF-014 pede explicitamente SSO via Keycloak como Should Have.

## Refinamentos posteriores

- O fallback de autenticação local mencionado na decisão inicial foi removido pelo [ADR-006](ADR-006-sem-fallback-auth-keycloak.md).
- O enforcement por projeto foi detalhado na implementação e nos artefatos de RBAC: permissões são resolvidas no contexto do projeto, com proteção adicional para `adminGlobal` no [ADR-007](ADR-007-bootstrap-admin-global.md).
