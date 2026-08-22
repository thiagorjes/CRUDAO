# TASK-00.1 — Provisionar Keycloak via Docker [M]

**Epic:** EPIC-00 — Infraestrutura Base | **User Story:** US-00.1 — Ambiente de desenvolvimento pronto
**Sistema:** CRUDAO | **RF:** RF-014 (dependência de RNF-003) | **Dependências:** nenhuma

---

## Contexto

Diretriz definida na techspec (seção 6): esta task deve ser uma das primeiras, antes de qualquer task de backend que dependa de autenticação/autorização, para não bloquear o fluxo de implementação.

## O que deve ser feito

- [ ] Subir instância local de Keycloak via `docker-compose` (`quay.io/keycloak/keycloak`)
- [ ] Criar realm `crudao` e client para a aplicação (Authorization Code Flow)
- [ ] Criar usuários e papéis de teste (admin, user)
- [ ] Validar claims retornadas (`sub`, `email`, `preferred_username`, `realm_access.roles`) contra o mock em `docs/contracts/CRUDAO-keycloak-mock-contract.md`
- [ ] Substituir o mock contract pelo contrato real (renomear e atualizar status para `ok`, conforme checklist do próprio arquivo)

## Guia técnico

- Arquivo: `docker-compose.yml` (novo serviço `keycloak`)
- Referência: `docs/contracts/CRUDAO-keycloak-mock-contract.md`, [ADR-003](../../decisions/ADR-003-rbac-hibrido-keycloak.md)

## Critérios de aceite

- Keycloak acessível localmente via Docker, com realm/client configurados
- Contrato de autenticação validado e documento renomeado para `CRUDAO-keycloak-contract.md` com status `ok`

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
