---
id: ADR-002
type: ADR
status: accepted
date: 2026-08-22
supersedes: —
superseded-by: —
---

# ADR-002 — PostgreSQL como único armazenamento; sem cache/broker nesta fase

## Decisão

PostgreSQL (via Docker) é o único armazenamento de dados na primeira entrega. Não haverá Redis ou outro cache/message broker nesta fase — o broadcast de eventos em tempo real (WebSocket/STOMP) entre múltiplos pods será resolvido inicialmente sem um broker compartilhado, sujeito a validação de escala (RNF-002: 1 pod obrigatório, 2+ pods desejável).

## Motivação

Escolha deliberada de simplicidade nesta fase: validar o produto antes de introduzir infraestrutura adicional (Redis/broker). A necessidade real de um broker de mensagens compartilhado só se confirma sob carga com múltiplos pods.

**Problema que resolve:**
Evita complexidade prematura de infraestrutura antes de validar a necessidade.

**Restrições consideradas:**
- RNF-002 exige funcionamento correto com 1 pod e escalabilidade para 2+ pods sem inconsistência.
- Sem cache disponível nesta fase, por decisão explícita do usuário.

## Consequências

**Positivas:**
- Menor complexidade operacional inicial.

**Negativas / trade-offs:**
- Em cenário de 2+ pods, broadcast de eventos WebSocket entre instâncias exige solução técnica a definir em techspec (ex.: PostgreSQL LISTEN/NOTIFY como mecanismo interino) — **risco técnico registrado como questão em aberto para a TechSpec.**

**Downstream afetado:**
- TechSpec deve detalhar o mecanismo de propagação de eventos entre pods sem broker dedicado.

## Alternativas Consideradas

### Alternativa 1 — Redis Pub/Sub desde o início
**Descartada porque:** usuário optou por testar sem cache primeiro e ajustar depois, conforme necessidade real observada.

### Alternativa 2 — Broker de mensagens dedicado (RabbitMQ/Kafka)
**Descartada porque:** overhead desnecessário para o escopo e escala atual do sistema.
