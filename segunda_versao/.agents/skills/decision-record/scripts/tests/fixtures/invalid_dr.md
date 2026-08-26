<!--
Fixture inválida — erros esperados ao rodar validate.py --mode output:
1. status: in-progress não pertence a [accepted, superseded, deprecated] (check_dr_status.py).
2. Seção obrigatória "## Alternativas Consideradas" ausente.
-->
---
id: ADR-098
type: ADR
status: in-progress
date: 2026-08-22
supersedes: —
superseded-by: —
---

# ADR-098 — Decisão de teste para fixture inválida

## Decisão

Fixture usada apenas para validar detecção de erros no schema de DR.

## Motivação

Contexto de teste.

**Problema que resolve:**
Garantir que status inválido é detectado.

**Restrições consideradas:**
- Nenhuma, é fixture de teste.

## Consequências

**Positivas:**
- Cobertura de teste.

**Negativas / trade-offs:**
- Nenhuma.

**Downstream afetado:**
- Nenhum.
