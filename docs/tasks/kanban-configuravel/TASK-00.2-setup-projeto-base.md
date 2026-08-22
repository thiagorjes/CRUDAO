# TASK-00.2 — Setup do projeto base (backend, frontend, PostgreSQL) [G]

**Epic:** EPIC-00 — Infraestrutura Base | **User Story:** US-00.1 — Ambiente de desenvolvimento pronto
**Sistema:** CRUDAO | **RF:** — (infraestrutura) | **Dependências:** TASK-00.1

---

## Contexto

Estabelecer o esqueleto do backend Spring Boot e do frontend Next.js, com PostgreSQL via Docker, seguindo `systems/CRUDAO/guidelines/stack.md` e `architecture.md`.

## O que deve ser feito

- [ ] Criar projeto Spring Boot (Java 25) com Spring Data JPA/Hibernate, Lombok, MapStruct, Bean Validation, WebSocket/STOMP, client OIDC
- [ ] Configurar PostgreSQL via `docker-compose`
- [ ] Configurar Spring Security com client OIDC apontando para o Keycloak da TASK-00.1
- [ ] Criar projeto Next.js (Node LTS) com estrutura inicial de páginas
- [ ] Configurar linters: Spotless+Checkstyle (backend), ESLint+Prettier (frontend)
- [ ] Configurar logging em arquivo com rotação a cada 5MB, retendo os 10 últimos (guidelines/observability.md)
- [ ] Configurar estrutura de testes: JUnit 5 + Testcontainers (backend), Jest/Vitest + Testing Library (frontend)

## Guia técnico

- Arquivos: `backend/pom.xml` (ou `build.gradle`), `frontend/package.json`, `docker-compose.yml`
- Referência: `systems/CRUDAO/guidelines/stack.md`, `architecture.md`, `git-workflow.md`

## Critérios de aceite

- Backend sobe localmente via Docker e conecta ao PostgreSQL e ao Keycloak
- Frontend sobe localmente e faz uma chamada de exemplo à API do backend
- Lint e testes rodam localmente sem erro (mesmo com suíte vazia/mínima)

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
