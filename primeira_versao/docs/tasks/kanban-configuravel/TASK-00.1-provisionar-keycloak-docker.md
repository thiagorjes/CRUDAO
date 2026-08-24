# TASK-00.1 — Provisionar Keycloak via Docker [M]

**Epic:** EPIC-00 — Infraestrutura Base | **User Story:** US-00.1 — Ambiente de desenvolvimento pronto
**Sistema:** CRUDAO | **RF:** RF-014 (dependência de RNF-003) | **Dependências:** nenhuma

---

## Contexto

Diretriz definida na techspec (seção 6): esta task deve ser uma das primeiras, antes de qualquer task de backend que dependa de autenticação/autorização, para não bloquear o fluxo de implementação.

## O que deve ser feito

- [x] Subir instância local de Keycloak via `docker-compose` (`quay.io/keycloak/keycloak:26.0`)
- [x] Criar realm `crudao` e client `crudao-app` (Authorization Code Flow)
- [x] Criar usuários e papéis de teste (admin, user)
- [x] Validar claims retornadas (`sub`, `email`, `preferred_username`, `realm_access.roles`) contra o mock em `docs/contracts/CRUDAO-keycloak-mock-contract.md`
- [x] Substituir o mock contract pelo contrato real (`docs/contracts/CRUDAO-keycloak-contract.md`, status `ok`)

## Guia técnico

- Arquivo: `docker-compose.yml` (novo serviço `keycloak`)
- Referência: `docs/contracts/CRUDAO-keycloak-mock-contract.md`, [ADR-003](../../decisions/ADR-003-rbac-hibrido-keycloak.md)

## Critérios de aceite

- [x] Keycloak acessível localmente via Docker, com realm/client configurados
- [x] Contrato de autenticação validado e documento renomeado para `CRUDAO-keycloak-contract.md` com status `ok`

## Status: Concluída — 2026-08-22

Validado via `docker compose up -d keycloak` + `password grant` de teste; claims conferidas (ver `docs/contracts/CRUDAO-keycloak-contract.md`). Nota registrada para TASK-04.1: `realm_access.roles` vem no `access_token`, não no `id_token`.

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
