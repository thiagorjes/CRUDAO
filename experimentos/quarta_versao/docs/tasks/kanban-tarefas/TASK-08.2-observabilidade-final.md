# TASK-08.2 — Observabilidade final

**Tamanho:** [P] ≤4h
**Sistema:** CRUDAO
**RF de origem:** RNF-002, RNF-004
**Dependências:** TASK-05.3
**Paralelismo:** [P] com TASK-08.1

## Contexto

Fecha os requisitos de `observability.md` não cobertos incrementalmente pelas tasks anteriores.

## O que deve ser feito

- [ ] Confirmar logging em arquivo local (rotação 5MB, retenção 10 arquivos).
- [ ] Confirmar métricas mínimas via Actuator/Micrometer completas (reconexões, latência NOTIFY→STOMP).
- [ ] Produzir stub de runbook operacional de indisponibilidade do Keycloak (referenciado na TechSpec como pré-requisito de go-live, fora do escopo funcional).

## Guia técnico

- `backend/src/main/resources/logback-spring.xml`
- `docs/runbooks/keycloak-indisponivel.md` (novo)

## Critérios de aceite

- Logs rotacionam conforme especificado.
- Métricas visíveis via `/actuator/metrics`.
- Runbook stub existe e cobre: sintoma, verificação, escalonamento.

## Status: Concluída — 2026-08-26

- `logback-spring.xml` criado (novo): appender de arquivo `RollingFileAppender` + `FixedWindowRollingPolicy` (maxIndex 10) + `SizeBasedTriggeringPolicy` (5MB), console mantido em paralelo.
- `application.yml`: `management.endpoints.web.exposure.include` ganhou `metrics` (health/info já expostos desde TASK-02.1/05.3) — expõe os contadores/timers `kanban.evento.listener.reconexoes`/`kanban.evento.listener.latencia` já criados em `AbstractPgListener` (TASK-05.3), sem métrica nova nesta task.
- `docs/runbooks/keycloak-indisponivel.md` criado (novo) — sintoma, verificação, escalonamento (ADR-006 referenciado).
- Testes: `mvn test -Dtest="*Test"` — **165/165 verdes**, sem regressão.
- **Code review:** agent QA (contexto fresco, general-purpose) — 0 findings 🔴. 2 findings 🟡 corrigidos: (1) `maxIndex=10` em `logback-spring.xml` retinha 11 arquivos (1 ativo + 10 rotacionados) em vez de 10 — corrigido para `maxIndex=9`; (2) `logs/` sem entrada em `.gitignore` do backend, risco de commit acidental de log local — adicionado. 3 findings 🟢 não corrigidos (decisão, baixo risco): `/actuator/metrics` autenticado mas sem restrição de papel (registrar como guardrail futuro no canvas); `LOG_FILE` com caminho relativo (infra de deploy, fora de escopo); sem teste automatizado (esperado, task config-only).
