# TASK-08.2 — Observabilidade final

**Tamanho:** [P] ≤4h
**Sistema:** CRUDAO
**RF de origem:** RNF-002, RNF-004
**Dependências:** TASK-05.3
**Paralelismo:** [P] com TASK-08.1

## Contexto

Fecha os requisitos de `observability.md` não cobertos incrementalmente pelas tasks anteriores.

## O que deve ser feito

- [x] Confirmar logging em arquivo local (rotação 5MB, retenção 10 arquivos).
- [x] Confirmar métricas mínimas via Actuator/Micrometer completas (reconexões, latência NOTIFY→STOMP).
- [x] Produzir stub de runbook operacional de indisponibilidade do Keycloak (referenciado na TechSpec como pré-requisito de go-live, fora do escopo funcional).

## Guia técnico

- `backend/src/main/resources/logback-spring.xml`
- `docs/runbooks/keycloak-indisponivel.md` (novo)

## Critérios de aceite

- Logs rotacionam conforme especificado.
- Métricas visíveis via `/actuator/metrics`.
- Runbook stub existe e cobre: sintoma, verificação, escalonamento.

## Status: Concluída — 2026-09-01

- `backend/src/main/resources/logback-spring.xml` criado: `RollingFileAppender` em
  `${LOG_DIR:-logs}/kanban-backend.log` + `FixedWindowRollingPolicy` (`minIndex 1`, `maxIndex 9`
  → 10 arquivos no total contando o ativo) + `SizeBasedTriggeringPolicy` `5MB`; `ConsoleAppender`
  mantido para `docker logs`. `include` dos defaults do Spring Boot para os patterns. Boot
  verificado no compose: `logs/kanban-backend.log` criado, app sobe em ~10s.
- Métricas: `application.yml` já expunha `health,info,metrics`. Os meters exigidos já existem em
  `AbstractListenNotifyRelay` (TASK-05.3): `kanban.listener.reconnections` (Counter, tag `canal`) e
  `kanban.listener.notify_to_stomp` (Timer, tag `canal`) — registrados no construtor, um par por
  canal (`board_events`, `notificacao_events`). Nenhuma métrica nova nesta task.
  `GET /actuator/metrics` responde `403` sem autenticação **por decisão de TASK-08.3**
  (`SecurityConfig`: só `/actuator/health/**` é `permitAll`, o resto exige sessão autenticada para
  não vazar detalhes internos) — endpoint exposto e meters registrados; acesso é autenticado.
- `docs/runbooks/keycloak-indisponivel.md` criado (stub) — contexto (ADR-006, sem fallback),
  sintoma, verificação, ação imediata, tabela de escalonamento N1/N2/N3, pendências do stub.
- `logs/` já está em `systems/CRUDAO/backend/.gitignore`.
- Sem teste automatizado (task config/doc-only). Suíte de integração sem regressão (rebuild do
  backend no compose sobe e fica `healthy`; `listenNotify` UP em ambos os canais).
