# TASK-04.1 — RBAC híbrido: papéis, permissões e integração Keycloak [G]

**Epic:** EPIC-04 — RBAC e Autenticação | **User Story:** US-04.1 — Controle de acesso configurável
**Sistema:** CRUDAO | **RF:** RF-013, RF-014, RNF-003 | **Dependências:** TASK-00.2

---

## Contexto

Keycloak autentica; a aplicação modela papéis/permissões configuráveis pelo admin em runtime ([ADR-003](../../decisions/ADR-003-rbac-hibrido-keycloak.md)), com o papel `admin` protegido (RN-006).

## O que deve ser feito

- [ ] Implementar entidades Usuário, Papel, Permissão, PapelPermissao
- [ ] Seed dos papéis padrão `admin` (protegido) e `user`
- [ ] CRUD de Papel/Permissão pelo admin (ou papel delegado, exceto sobre o próprio papel `admin`, RN-006)
- [ ] Definir granularidade final das chaves de permissão (Q-003 da techspec: ex. `projeto:gerenciar`, `workflow:gerenciar`, `tarefa:gerenciar`, `impedimento:marcar`, `papel:gerenciar`, `dashboard:visualizar`)
- [ ] Middleware/aspecto de validação de permissão em todo endpoint de escrita (RNF-003)
- [ ] Integração OIDC completa com o Keycloak da TASK-00.1, mapeando claim/sub do usuário autenticado ao Usuário interno (RF-014)
- [ ] Implementar fallback de autenticação própria caso Keycloak esteja indisponível

## Guia técnico

- Pacote: `security/`, `domain/rbac`
- Referência: [ADR-003](../../decisions/ADR-003-rbac-hibrido-keycloak.md), `docs/contracts/CRUDAO-keycloak-contract.md` (validado na TASK-00.1)

## Critérios de aceite

- Usuário autenticado via Keycloak tem seu papel/permissões aplicados corretamente
- Tentativa de alterar o papel `admin` por um papel delegado é bloqueada (teste unitário, RN-006)
- Todo endpoint de escrita rejeita requisição sem a permissão necessária

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
