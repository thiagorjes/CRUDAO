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
