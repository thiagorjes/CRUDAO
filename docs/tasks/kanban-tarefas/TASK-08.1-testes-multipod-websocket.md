# TASK-08.1 — Testes multi-pod e WebSocket (RNF-001/RNF-002)

**Tamanho:** [M] 4-8h
**Sistema:** CRUDAO
**RF de origem:** RNF-001, RNF-002
**Dependências:** TASK-05.3, TASK-05.2
**Paralelismo:** [P] com TASK-08.2

## Contexto

Validação formal do requisito de escalabilidade horizontal sem inconsistência.

## O que deve ser feito

- [ ] Teste de integração com 2 instâncias Spring Boot compartilhando o mesmo PostgreSQL Testcontainer — evento publicado via pod A deve chegar ao cliente STOMP conectado ao pod B.
- [ ] Teste de integração com 2 conexões WebSocket simuladas validando propagação do evento em <2s (`Awaitility`).
- [ ] Teste de resincronização client-side por gap de `seq`.
- [ ] Teste de notificação multi-pod: `Notificacao` gerada por evento processado no pod A chega ao cliente STOMP conectado ao pod B em `/topic/notificacoes/{usuarioId}` (RF-005 sob RNF-002).

## Guia técnico

- `backend/src/test/java/.../multipod/`

## Critérios de aceite

- Todos os testes acima passam de forma determinística: 0 falhas em 10 execuções consecutivas locais (amostra fixa — critério substitui o percentual vago de flakiness).
