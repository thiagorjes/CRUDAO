---
id: ADR-005
type: ADR
status: accepted
date: 2026-08-22
supersedes: —
superseded-by: —
---

# ADR-005 — Cálculo do dashboard de gestão de forma assíncrona

## Decisão

O endpoint que aciona o cálculo agregado do dashboard (lead-time médio por etapa e tempo médio em impedimento, RF-007) dispara o processamento em background (`@Async` do Spring) e responde imediatamente com um identificador de job; o frontend recebe o resultado via WebSocket/STOMP (mesmo canal usado para tempo real) quando o cálculo terminar, com fallback de polling caso o WebSocket não esteja disponível.

## Motivação

Consultas agregadas sobre período configurável (intervalo de datas) podem varrer grande volume de registros de lead-time, correndo risco de timeout HTTP no frontend em requisições síncronas.

**Problema que resolve:**
Evitar timeout de requisição HTTP ao calcular métricas agregadas sobre períodos potencialmente longos (RF-007, clarificado em /clarify).

**Restrições consideradas:**
- Sem cache/broker dedicado nesta fase (ADR-002) — o próprio canal WebSocket/STOMP existente é reaproveitado para entregar o resultado.

## Consequências

**Positivas:**
- Requisição HTTP inicial responde rápido, sem bloquear a UI.
- Reaproveita a infraestrutura de tempo real já definida (ADR-001, ADR-004).

**Negativas / trade-offs:**
- UI precisa de estado de "carregando" e tratamento de job assíncrono (id, status, resultado).

**Downstream afetado:**
- Frontend: tela de dashboard precisa lidar com resposta assíncrona.
- Backend: necessidade de um `@Async` executor dedicado, dimensionado para não competir com o processamento de eventos em tempo real.

## Alternativas Consideradas

### Alternativa 1 — Cálculo síncrono com paginação
**Descartada porque:** não resolve o risco de timeout para períodos longos sem também exigir múltiplas chamadas do frontend, complicando ainda mais a UX.

### Alternativa 2 — Cálculo pré-agregado (materialized view atualizada por trigger)
**Descartada por ora:** adiciona complexidade de manutenção de view; pode ser reavaliada se o processamento assíncrono não atender ao desempenho esperado.
