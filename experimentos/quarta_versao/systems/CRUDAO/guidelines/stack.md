# Stack — CRUDAO
_Versão: 1.0 | Data: 2026-08-22_

> Decisões relacionadas: [ADR-001](../../../docs/decisions/ADR-001-stack-backend-java-spring.md), [ADR-002](../../../docs/decisions/ADR-002-postgresql-sem-cache-tempo-real.md)

## Backend

- **Linguagem:** Java 25, executado via imagem Docker (JDK 25) — build via `maven:3.9-eclipse-temurin-25`, runtime via `eclipse-temurin:25-jre` (ver `backend/Dockerfile`).
- **Framework:** Spring Boot **3.5.16** (fixado na TASK-00.2 — versões < 3.5 não geram bytecode compatível com Java 25 no `spring-boot-maven-plugin`).
- **Lombok:** **1.18.46** (fixado — versões anteriores incompatíveis com o `javac` do JDK 25).
- **Spotless Maven Plugin:** **3.10.0** (fixado pelo mesmo motivo — Google Java Format via javac interno).
- **Persistência:** Spring Data JPA / Hibernate (a definir entre os dois durante implementação, priorizando compatibilidade e performance).
- **Tempo real:** WebSocket com protocolo STOMP (`spring-boot-starter-websocket`).
- **Autenticação:** Client OIDC (Spring Security OAuth2/OIDC) integrado a Keycloak.
- **Bibliotecas obrigatórias:** Lombok, MapStruct (DTO mapping), Bean Validation (Jakarta Validation).

## Frontend

- **Framework:** Next.js.
- **Runtime:** Node.js LTS mais recente, exclusivamente dentro da imagem Docker do frontend.

## Banco de dados

- **Principal:** PostgreSQL, executado via Docker.
- **Cache/broker:** nenhum nesta fase (ver ADR-002). Reavaliar conforme necessidade real de escala.

## Infraestrutura

- **Execução e deploy:** todos os componentes executáveis (backend, frontend, Keycloak e PostgreSQL) devem rodar em containers Docker no desenvolvimento e homologação. OpenShift/Kubernetes são alvos futuros de orquestração (mencionados no PRD como requisito de portabilidade).
- **CI/CD:** nenhum pipeline automatizado nesta fase — build, testes e lint executados localmente antes de commit/push.
