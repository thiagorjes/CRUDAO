---
id: ADR-001
type: ADR
status: accepted
date: 2026-08-22
supersedes: —
superseded-by: —
---

# ADR-001 — Stack de backend: Java 25 + Spring Boot (LTS)

## Decisão

Backend implementado em Java 25, com Spring Boot na versão LTS mais recente, Spring Data JPA/Hibernate para persistência, PostgreSQL como banco relacional, WebSocket/STOMP para atualização em tempo real e client OIDC para integração com Keycloak.

## Motivação

Stack já dominada pela equipe, com forte suporte a APIs REST, WebSocket nativo (STOMP) e integração OIDC madura via Spring Security. Atende ao RNF-001 (tempo real <2s) e RNF-003 (controle de acesso por papel).

**Problema que resolve:**

Necessidade de uma base robusta para board colaborativo em tempo real com múltiplos usuários simultâneos e controle de acesso configurável.

**Restrições consideradas:**

- Ambiente on-premise, tudo containerizado via Docker.
- Sem pipeline de CI/CD definido nesta fase.

## Consequências

**Positivas:**

- Ecossistema maduro para REST, WebSocket, segurança e persistência.
- LTS reduz custo de manutenção de versão.

**Negativas / trade-offs:**

- WebSocket/STOMP em múltiplos pods sem cache/broker compartilhado (ver ADR-002) exige atenção ao broadcast entre instâncias.

**Downstream afetado:**

- TechSpec: definição de módulos, camadas e contratos de API.

## Alternativas Consideradas

### Alternativa 1 — Node.js/NestJS no backend

**Descartada porque:** equipe já domina Java/Spring; unificação de stack reduz curva de aprendizado.

### Alternativa 2 — Kotlin sobre Spring Boot

**Descartada porque:** sem ganho relevante para o escopo atual; Java 25 já cobre as necessidades.

## Refinamentos posteriores

- Spring Boot foi fixado em 3.5.16 e as versões de Lombok e Spotless foram fixadas para compatibilidade com Java 25.
- O empacotamento em Docker foi detalhado no [ADR-008](ADR-008-dockerizacao-backend-frontend.md).
