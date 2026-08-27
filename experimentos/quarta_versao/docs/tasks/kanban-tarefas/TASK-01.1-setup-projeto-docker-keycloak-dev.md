# TASK-01.1 — Setup de projeto backend/frontend + docker-compose + Keycloak dev

**Status:** Concluída — 2026-08-27
**Tamanho:** [G] 1-2 dias
**Sistema:** CRUDAO
**RF de origem:** RNF-004 (pré-requisito de todas as demais tasks)
**Dependências:** nenhuma
**Paralelismo:** nenhum (bloqueante de tudo)

## Contexto

Base de execução local para todo o desenvolvimento subsequente. Sem isso, nenhuma outra task pode ser validada localmente (Postgres + Keycloak são dependências diretas de quase todo RF). Confirmado com o usuário na Fase 1 de `/tasks`: o setup do Keycloak para **ambiente local de desenvolvimento** (realm/client) está em escopo desta feature — o Keycloak (servidor/IdP) em si é premissa externa já disponível em produção (PRD Seção 7, ADR-006), mas o container/realm de dev precisa ser provisionado pela equipe, conforme `docs/techspec/kanban-tarefas/quickstart.md`.

## O que deve ser feito

- [x] Criar esqueleto Spring Boot 3.5.16 / Java 25 (`backend/`) com dependências: Web, Data JPA, OAuth2 Client/Resource Server, WebSocket, Validation, Flyway, Actuator, MapStruct.
- [x] Criar esqueleto Next.js (`frontend/`).
- [x] Criar `docker-compose.yml` na raiz do sistema com serviços `postgres` e `keycloak` (imagem oficial, modo dev).
- [x] Criar realm export do Keycloak (`keycloak/realm-export.json`) com client OIDC configurado, redirect URI `http://localhost:3000/login/oauth2/code/keycloak`, e ao menos 2 usuários de teste (um por papel dev/admin) — importado automaticamente no boot do container.
- [x] Configurar `application.yml` (dev profile) apontando para Postgres/Keycloak locais.
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

- `docker compose up postgres keycloak` sobe os dois serviços saudáveis.
- Backend inicia (`./mvnw spring-boot:run`) e conecta ao Postgres sem erro.
- Frontend inicia (`npm run dev`) servindo página padrão.
- Realm importado automaticamente contém client e usuários de teste — validado por login manual no Keycloak admin console.
