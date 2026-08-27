# Architecture — CRUDAO
_Versão: 1.0 | Data: 2026-08-22_

> Decisões relacionadas: [ADR-001](../../../docs/decisions/ADR-001-stack-backend-java-spring.md), [ADR-002](../../../docs/decisions/ADR-002-postgresql-sem-cache-tempo-real.md), [ADR-003](../../../docs/decisions/ADR-003-rbac-hibrido-keycloak.md)

## Visão geral

Aplicação web com backend Spring Boot (API REST + WebSocket/STOMP) e frontend Next.js, autenticação via Keycloak (OIDC), persistência em PostgreSQL. Deploy containerizado on-premise via Docker.

## Camadas (backend)

- **Controller:** exposição de endpoints REST e handlers WebSocket/STOMP.
- **Service:** regras de negócio (transições de workflow, cálculo de lead-time, controle de permissões).
- **Repository:** acesso a dados via Spring Data JPA.
- **DTO/Mapper:** MapStruct para conversão entre entidades e DTOs expostos na API.

## Tempo real

Eventos de mudança de estado (movimentação de card, impedimento, etc.) são publicados via STOMP para os clientes conectados ao board/projeto correspondente, respeitando o limiar de latência de RNF-001 (<2s).

**Questão em aberto (a resolver em techspec):** mecanismo de broadcast de eventos entre múltiplos pods sem broker dedicado (ver ADR-002) — candidato inicial: PostgreSQL `LISTEN/NOTIFY`.

## Multi-instância (escalabilidade)

Deve ser possível rodar com 1 pod (padrão) e escalar para 2+ pods sem inconsistência de estado (RNF-002). Toda lógica de estado deve residir no PostgreSQL (fonte única da verdade), evitando estado em memória não compartilhado entre instâncias.

## Frontend

Next.js consumindo a API REST do backend e conectando ao WebSocket/STOMP para atualizações em tempo real. Responsivo para desktop, compatível com navegadores modernos.
