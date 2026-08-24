---
id: ADR-004
type: ADR
status: accepted
date: 2026-08-22
supersedes: —
superseded-by: —
---

# ADR-004 — Broadcast de eventos entre pods via PostgreSQL LISTEN/NOTIFY

## Decisão

O broadcast de eventos em tempo real (movimentação de card, impedimento, notificações) entre múltiplas instâncias (pods) do backend usa o mecanismo `LISTEN/NOTIFY` do PostgreSQL: cada pod publica o evento no banco e todos os pods (inscritos no canal) recebem a notificação e retransmitem via WebSocket/STOMP aos clientes conectados localmente.

## Motivação

Resolve a questão em aberto registrada em ADR-002 sem introduzir um broker dedicado (Redis/RabbitMQ) nesta fase, mantendo a stack simples enquanto valida a necessidade real de escala.

**Problema que resolve:**
Garantir que um evento originado em um pod chegue aos clientes conectados a outro pod, mantendo consistência de estado do board entre todas as instâncias (RNF-002).

**Restrições consideradas:**
- Sem Redis/broker disponível nesta fase (ADR-002).
- Limiar de latência de 2s (RNF-001) deve ser respeitado mesmo com o salto adicional via banco.

## Consequências

**Positivas:**
- Nenhuma infraestrutura nova além do PostgreSQL já usado.
- Consistência garantida pela mesma fonte de verdade dos dados.

**Negativas / trade-offs:**
- `LISTEN/NOTIFY` tem limite de payload (8KB) — eventos devem carregar apenas IDs/metadados mínimos, exigindo um fetch adicional pelo pod receptor para montar o payload completo ao cliente.
- Sob volume alto, pode não escalar tão bem quanto um broker dedicado — throughput deve ser observado; se necessário, migrar para Redis Pub/Sub (ver ADR-002).

**Downstream afetado:**
- Implementação: listener assíncrono no backend, conectado permanentemente ao canal Postgres.

## Alternativas Consideradas

### Alternativa 1 — Redis Pub/Sub
**Descartada por ora:** introduz infraestrutura adicional antes de confirmar a necessidade real (ver ADR-002); pode ser adotado depois se `LISTEN/NOTIFY` não escalar.

### Alternativa 2 — Sticky sessions (afinidade de pod por usuário)
**Descartada porque:** não resolve o caso de dois usuários no mesmo board conectados a pods diferentes — ainda seria necessário propagar o evento entre pods.
