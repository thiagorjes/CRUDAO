---
id: ADR-099
type: ADR
status: accepted
date: 2026-08-22
supersedes: —
superseded-by: —
---

# ADR-099 — Decisão de teste para fixture válida

## Decisão

Usar esta fixture apenas para validar o schema de Decision Records.

## Motivação

Contexto de teste, sem restrições reais envolvidas.

**Problema que resolve:**
Garantir cobertura de teste do validate-rules.json de decision-record.

**Restrições consideradas:**
- Nenhuma, é fixture de teste.

## Consequências

**Positivas:**
- Cobertura de teste do schema de DR.

**Negativas / trade-offs:**
- Nenhuma.

**Downstream afetado:**
- Nenhum.

## Alternativas Consideradas

### Alternativa 1 — Não criar fixture
**Descartada porque:** deixaria o schema de DR sem cobertura de teste.

### Alternativa 2 — Reaproveitar fixture de outra skill
**Descartada porque:** DR tem schema próprio (frontmatter com status).
