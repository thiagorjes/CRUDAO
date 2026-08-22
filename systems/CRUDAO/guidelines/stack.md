# Stack — CRUDAO
_Versão: 1.0 | Data: 2026-08-22_

> Decisões relacionadas: [ADR-001](../../../docs/decisions/ADR-001-stack-backend-java-spring.md), [ADR-002](../../../docs/decisions/ADR-002-postgresql-sem-cache-tempo-real.md)

## Backend

- **Linguagem:** Java 25, executado via imagem Docker (JDK 25).
- **Framework:** Spring Boot (versão LTS mais recente disponível no momento do desenvolvimento).
- **Persistência:** Spring Data JPA / Hibernate (a definir entre os dois durante implementação, priorizando compatibilidade e performance).
- **Tempo real:** WebSocket com protocolo STOMP (`spring-boot-starter-websocket`).
- **Autenticação:** Client OIDC (Spring Security OAuth2/OIDC) integrado a Keycloak.
- **Bibliotecas obrigatórias:** Lombok, MapStruct (DTO mapping), Bean Validation (Jakarta Validation).

## Frontend

- **Framework:** Next.js.
- **Runtime:** Node.js LTS mais recente.

## Banco de dados

- **Principal:** PostgreSQL, executado via Docker.
- **Cache/broker:** nenhum nesta fase (ver ADR-002). Reavaliar conforme necessidade real de escala.

## Infraestrutura

- **Deploy:** containerizado via Docker, ambiente on-premise. OpenShift/Kubernetes como alvo futuro de orquestração (mencionado no PRD como requisito de portabilidade).
- **CI/CD:** nenhum pipeline automatizado nesta fase — build, testes e lint executados localmente antes de commit/push.
