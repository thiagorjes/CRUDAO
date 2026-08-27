# TASK-01.1 — Setup de projeto backend/frontend + docker-compose + Keycloak dev

**Status:** Bloqueada — review reprovado em 2026-08-27
**Tamanho:** [G] 1-2 dias
**Sistema:** CRUDAO
**RF de origem:** RNF-004 (pré-requisito de todas as demais tasks)
**Dependências:** nenhuma
**Paralelismo:** nenhum (bloqueante de tudo)

## Contexto

Base de execução containerizada para todo o desenvolvimento subsequente. Sem a stack Docker, nenhuma outra task pode ser validada (Postgres e Keycloak são dependências diretas de quase todo RF). O setup do Keycloak para **ambiente de desenvolvimento** (realm/client) está em escopo desta feature; o container e o realm de dev precisam ser provisionados pela equipe, conforme `docs/techspec/kanban-tarefas/quickstart.md`.

## O que deve ser feito

- [x] Criar esqueleto Spring Boot 3.5.16 / Java 25 (`backend/`) com dependências: Web, Data JPA, OAuth2 Client/Resource Server, WebSocket, Validation, Flyway, Actuator, MapStruct.
- [x] Criar esqueleto Next.js (`frontend/`).
- [ ] Criar `docker-compose.yml` na raiz do sistema com serviços `postgres`, `keycloak`, `backend` e `frontend` (imagens oficiais/multi-stage, modo dev).
- [x] Criar realm export do Keycloak (`keycloak/realm-export.json`) com client OIDC configurado, redirect URI `http://localhost:3000/login/oauth2/code/keycloak`, e ao menos 2 usuários de teste (um por papel dev/admin) — importado automaticamente no boot do container.
- [ ] Configurar `application.yml` (dev profile) apontando para os serviços Postgres/Keycloak da rede Docker.
- [x] Configurar Dockerfile do backend e do frontend (RNF-004).

## Guia técnico

Todo caminho relativo a `systems/CRUDAO/`.

- `backend/pom.xml` — dependências conforme `stack.md`.
- `backend/src/main/resources/application.yml` — profile `dev`.
- `docker-compose.yml` — serviços `postgres`, `keycloak`.
- `keycloak/realm-export.json` — realm de dev.
- `frontend/package.json` — projeto Next.js.
- Seguir `architecture.md` para estrutura de pacotes do backend.

## Critérios de aceite

- `docker compose up -d` sobe PostgreSQL, Keycloak, backend e frontend como serviços saudáveis.
- Backend inicia dentro do container Docker e conecta ao PostgreSQL sem erro.
- Frontend inicia dentro do container Docker e serve a página padrão.
- Realm importado automaticamente contém client e usuários de teste — validado por login manual no Keycloak admin console.
