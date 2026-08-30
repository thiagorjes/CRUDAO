# TASK-05.3 — Resiliência: reconexão do listener, resincronização client, health-check, métricas

**Status:** Concluída — 2026-08-29 (implementação real; marcação anterior de 2026-08-26 era resíduo de template)

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

## Implementação (2026-08-29)

- `evento/adapter/AbstractListenNotifyRelay.java` (novo) — base comum dos 2 adapters: loop `LISTEN` com **reconexão infinita** e backoff exponencial (1s→teto 30s), logs progressivos WARN→ERROR (>3 tentativas), estado `conectado`, métricas Micrometer `kanban.listener.reconnections{canal}` (contador) e `kanban.listener.notify_to_stomp{canal}` (timer). Envelope do NOTIFY ganhou `ts` (epochMillis) p/ medir latência.
- `ListenNotifyPublisher` / `ListenNotifyNotificacaoPublisher` — refatorados p/ estender a base (só serialização + canal + destino STOMP).
- `health/ListenNotifyHealthIndicator.java` (novo, `@Component("listenNotify")`) — DOWN se qualquer relay desconectado; detalhe por canal.
- `application.yml` — expõe `health,info,metrics`; `health.probes.enabled`; grupo `readiness` = `readinessState,listenNotify,keycloak`.
- Testes: `AbstractListenNotifyRelayBackoffTest` (4), `ListenNotifyHealthIndicatorTest` (2), `ListenNotifyReconexaoIntegrationTest` (1, IT: mata o backend Postgres do `LISTEN`, verifica reconexão + contador de reconexões + propagação do `NOTIFY` seguinte). Suíte `-P integration-tests`: **152 testes, 0 falhas**.

### Ajustes pós code-review (2026-08-30)

- IT de reconexão-após-kill adicionado (era o critério de aceite sem cobertura automatizada).
- Métrica `kanban.listener.reconnections` passou a contar **reconexão bem-sucedida**, não tentativa falha (não infla durante indisponibilidade prolongada).
- Truncamento de payload >8KB não corta mais o JSON (envelope inválido); publica `{"tipo":..,"projetoId|usuarioId":..,"truncado":true}` — válido e dispara o resync do cliente.
- Limite de payload medido em bytes UTF-8 (`getBytes(StandardCharsets.UTF_8)`).
- Canvas S sincronizado (reconexão "até 10 tentativas" → infinita com backoff + readiness).

## Critérios de aceite

- Kill da conexão JDBC do listener em execução → reconecta com backoff; próximo `NOTIFY` ainda propaga.
- Readiness probe reporta não saudável durante desconexão.
- Métricas de reconexão e latência disponíveis via Actuator.
- Logs de reconexão em nível progressivo conforme falhas se acumulam.
