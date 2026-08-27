# TASK-05.3 — Resiliência: reconexão do listener, resincronização client, health-check, métricas

**Status:** Concluída — 2026-08-26

**Tamanho:** [M] 4-8h
**Sistema:** CRUDAO
**RF de origem:** RNF-001, RNF-002
**Dependências:** TASK-05.1
**Paralelismo:** nenhum

## Contexto

Mitiga o trade-off aceito de LISTEN/NOTIFY (sem replay de eventos perdidos, payload limitado a 8KB) — rede de segurança operacional ([ADR-004](../../decisions/ADR-004-broadcast-listen-notify.md)).

## O que deve ser feito

- [x] Implementar reconexão automática com backoff da conexão JDBC do listener em caso de queda. (já em TASK-05.1, confirmado)
- [x] Confirmar o `seq` incremental no payload do evento (já incluído em TASK-05.1); documentar contrato de resincronização client-side (frontend detecta gap ou reconexão de WebSocket e refaz `GET /board` — implementação de frontend em TASK-07.2).
- [x] Readiness probe (Actuator) reflete listener desconectado como não saudável.
- [x] Métricas Micrometer: contador de reconexões do listener por pod, latência entre `NOTIFY` e broadcast STOMP.
- [x] Logs progressivos (`WARN`→`ERROR`) em falha de reconexão. (já em TASK-05.1, confirmado)

## Guia técnico

- `backend/src/main/java/.../evento/adapter/ListenNotifyPublisher.java` (extensão de resiliência).
- `backend/src/main/java/.../health/ListenerHealthIndicator.java`.
- `backend/src/main/java/.../metrics/` — contadores Micrometer.

## Critérios de aceite

- Kill da conexão JDBC do listener em execução → reconecta com backoff; próximo `NOTIFY` ainda propaga.
- Readiness probe reporta não saudável durante desconexão.
- Métricas de reconexão e latência disponíveis via Actuator.
- Logs de reconexão em nível progressivo conforme falhas se acumulam.
