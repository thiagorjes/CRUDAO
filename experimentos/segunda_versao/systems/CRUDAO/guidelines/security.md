# Security — CRUDAO
_Versão: 1.0 | Data: 2026-08-22_

> Decisão relacionada: [ADR-003](../../../docs/decisions/ADR-003-rbac-hibrido-keycloak.md)

## Autenticação

- Keycloak (OIDC) como provedor de identidade (RF-014, Should Have).
- Fallback de autenticação própria caso Keycloak esteja indisponível — mecanismo a detalhar em techspec.

## Autorização

- Modelo RBAC híbrido: Keycloak autentica; papéis e permissões granulares são modelados e geridos dentro da aplicação (RF-013).
- Papel `admin` é protegido — não pode ser criado, editado ou excluído por nenhum papel delegado (RN-006).
- Toda ação de criação/edição/exclusão de entidades administrativas (projetos, workflows, colunas, raias, papéis) deve validar a permissão do usuário autenticado antes de executar.

## Compliance

- Sem requisito de compliance formal definido nesta fase (LGPD, SOC2 etc. não exigidos explicitamente). Boas práticas de OWASP Top 10 devem ser seguidas como baseline geral (validação de entrada, proteção contra injeção, gestão segura de sessão via OIDC).
