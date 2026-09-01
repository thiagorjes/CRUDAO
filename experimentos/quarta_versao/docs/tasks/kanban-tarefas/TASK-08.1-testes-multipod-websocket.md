# TASK-08.1 — Testes multi-pod e WebSocket (RNF-001/RNF-002)

**Status:** Concluída
**Tamanho:** [M] 4-8h
**Sistema:** CRUDAO
**RF de origem:** RNF-001, RNF-002
**Dependências:** TASK-05.3, TASK-05.2
**Paralelismo:** [P] com TASK-08.2

## Contexto

Validação formal do requisito de escalabilidade horizontal sem inconsistência.

## O que deve ser feito

- [x] Teste de integração com 2 instâncias Spring Boot compartilhando o mesmo PostgreSQL Testcontainer — evento publicado via pod A deve chegar ao cliente STOMP conectado ao pod B.
- [x] Teste de integração com 2 conexões WebSocket simuladas validando propagação do evento em <2s (`Awaitility`).
- [x] Teste de resincronização client-side por gap de `seq`.
- [x] Teste de notificação multi-pod: `Notificacao` gerada por evento processado no pod A chega ao cliente STOMP conectado ao pod B em `/topic/notificacoes/{usuarioId}` (RF-005 sob RNF-002).

## Guia técnico

- `backend/src/test/java/.../multipod/`

## Critérios de aceite

- Todos os testes acima passam de forma determinística: 0 falhas em 10 execuções consecutivas locais (amostra fixa — critério substitui o percentual vago de flakiness).

## Nota de implementação (2026-09-01)

- `MultiPodBroadcastIntegrationTest` (`backend/src/test/java/com/crudao/kanban/multipod/`): o pod A é o
  `@SpringBootTest(RANDOM_PORT, profile "it")`; o pod B é um segundo `ConfigurableApplicationContext`
  iniciado via `SpringApplicationBuilder`, apontando para o **mesmo Postgres** do stack Docker (banco
  `kanban_it`) — o datasource/keycloak/secret são resolvidos do `Environment` do pod A e repassados
  como args `--chave=valor` (precedência acima dos `application-*.yml`). Não usa Testcontainers: a
  suíte já roda contra o compose final (`run-integration-tests.ps1`), e Testcontainers foi removido
  do padrão do repo em TASK-05.x.
- Casos: (1) evento de board publicado no pod B → cliente STOMP do pod A recebe < 2s, `@RepeatedTest(10)`
  (gate de flakiness); (2) notificação publicada no pod B → cliente do pod A em `/topic/notificacoes/{id}`;
  (3) SUBSCRIBE no pod A e no pod B recebem o mesmo evento publicado uma única vez, ambos < 2s.
- Resincronização client-side por gap de `seq`: `frontend/stomp.test.ts` (lógica do `StompManager` —
  entrega em ordem sem resync, gap dispara `onRessinc` + `GET /board`, primeiro evento não é gap).
- Resultado: `mvn -P integration-tests test -Dtest=MultiPodBroadcastIntegrationTest` → 12/12 verde
  (10 repetições + 2); `vitest` → 8/8. Suíte completa `-P integration-tests`: 174 + repetições, sem regressão.
