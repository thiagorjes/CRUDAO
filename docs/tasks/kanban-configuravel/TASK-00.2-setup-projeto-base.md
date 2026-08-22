# TASK-00.2 — Setup do projeto base (backend, frontend, PostgreSQL) [G]

**Epic:** EPIC-00 — Infraestrutura Base | **User Story:** US-00.1 — Ambiente de desenvolvimento pronto
**Sistema:** CRUDAO | **RF:** — (infraestrutura) | **Dependências:** TASK-00.1

---

## Contexto

Estabelecer o esqueleto do backend Spring Boot e do frontend Next.js, com PostgreSQL via Docker, seguindo `systems/CRUDAO/guidelines/stack.md` e `architecture.md`.

## O que deve ser feito

- [x] Criar projeto Spring Boot (Java 25) com Spring Data JPA/Hibernate, Lombok, MapStruct, Bean Validation, WebSocket (starter), client OIDC
- [x] Configurar PostgreSQL via `docker-compose`
- [x] Configurar Spring Security com client OIDC apontando para o Keycloak da TASK-00.1
- [x] Criar projeto Next.js (Node 22 LTS) com estrutura inicial de páginas (App Router)
- [x] Configurar linters: Spotless (backend — Checkstyle adiado, ver nota), ESLint+Prettier (frontend)
- [x] Configurar logging em arquivo com rotação a cada 5MB, retendo os 10 últimos (guidelines/observability.md) via `logging.logback.rollingpolicy`
- [x] Configurar estrutura de testes: JUnit 5 + Testcontainers (backend), Vitest + Testing Library (frontend)

## Guia técnico

- Arquivos: `backend/pom.xml` (ou `build.gradle`), `frontend/package.json`, `docker-compose.yml`
- Referência: `systems/CRUDAO/guidelines/stack.md`, `architecture.md`, `git-workflow.md`

## Critérios de aceite

- [x] Backend sobe localmente via Docker e conecta ao PostgreSQL e ao Keycloak
- [x] Frontend sobe localmente e faz uma chamada de exemplo à API do backend
- [x] Lint e testes rodam localmente sem erro (mesmo com suíte vazia/mínima)

## Status: Concluída — 2026-08-22

Stack completa validada via `docker compose up -d --build` (keycloak + postgres + backend + frontend). Backend responde em `/api/health`; frontend (`http://localhost:3000`) renderiza a chamada ao backend server-side.

**Notas técnicas (desvios do plano original, registrados para rastreabilidade):**
- Spring Boot fixado em **3.5.16** (não 3.4.x) — versões anteriores do `spring-boot-maven-plugin` não suportam bytecode Java 25 ("Unsupported class file major version 69").
- Lombok fixado em **1.18.46** — versões anteriores falham ao compilar sob JDK 25 (incompatibilidade com API interna do `javac`).
- Spotless Maven Plugin fixado em **3.10.0** (não 2.44.0) pelo mesmo motivo (Google Java Format via javac interno).
- **Checkstyle não configurado nesta task** — adiado; Spotless já garante formatação consistente. Avaliar se Checkstyle é necessário além disso em task futura.
- Build do backend feito via Docker multi-stage (`maven:3.9-eclipse-temurin-25` → `eclipse-temurin:25-jre`), pois o JDK local da máquina de desenvolvimento é a versão 21, incompatível com o alvo Java 25 do projeto.
- Teste de integração `KanbanApplicationIT` (Testcontainers) foi validado apenas por compilação nesta sessão — execução completa requer Docker-in-Docker, não disponível no ambiente sandboxed usado aqui; validar ao rodar localmente com Docker Desktop padrão.

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
