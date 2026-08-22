---
name: analyze
description: Realiza análise cross-artefato entre PRD, TechSpec e Tasks no processo SDD, detectando inconsistências, lacunas de cobertura, ambiguidades e contradições. Use ao validar a consistência entre artefatos ou revisar especificações antes de iniciar a implementação.
canvas-dimensions: []
input-artifacts:
  - docs/prd/{{FEATURE}}-prd.md
  - docs/techspec/{{FEATURE}}-techspec.md
  - docs/tasks/{{FEATURE}}-tasks.md
  - docs/spdd/{{FEATURE}}-canvas.md
output-artifacts:
  - docs/analyze/{{FEATURE}}-analysis.md
---

## Objetivo

Verificar a consistência entre PRD, TechSpec, Tasks e o REASONS Canvas de uma feature antes de iniciar (ou prosseguir com) a implementação. Detecta RFs sem task correspondente, tasks sem RF de origem, divergências entre o canvas e a TechSpec, e contradições entre artefatos. Gera relatório agrupado por tipo de achado.

## Pré-condições

- `docs/prd/[feature]-prd.md` deve existir
- `docs/techspec/[feature]-techspec.md` deve existir
- `docs/tasks/[feature]-tasks.md` deve existir
- `docs/spdd/[feature]-canvas.md` — opcional; se ausente, pular verificação de divergência de canvas e informar no relatório

## Workflow

### Fase 0 — Leitura de contexto

Ler nesta ordem, sem pular:

1. `docs/prd/[feature]-prd.md` — extrair todos os RFs e RNFs
2. `docs/techspec/[feature]-techspec.md` — extrair decisões técnicas e Matriz de Rastreabilidade
3. `docs/tasks/[feature]-tasks.md` — extrair todas as TASKs e seus RFs de origem declarados
4. `docs/spdd/[feature]-canvas.md`, se existir — extrair dimensões preenchidas

### Fase 1 — Mapeamento RF → Task (gaps)

1. Construir o conjunto de RFs do PRD e o conjunto de RFs referenciados em tasks
2. Identificar RFs do PRD sem nenhuma task correspondente (**gap de cobertura**)
3. Identificar tasks sem RF de origem declarado (**task órfã**)
4. Executar `check_rf_coverage.py` como verificação automatizada complementar (não substitui a leitura manual — RFs mencionados só no texto sem tag podem escapar ao regex)

### Fase 2 — Mapeamento Canvas ↔ TechSpec (divergências)

Se o canvas existir, comparar dimensão por dimensão com a TechSpec:

| Dimensão do canvas | Comparar com |
|---|---|
| E — Entities | Data model da TechSpec |
| A — Approach | Seção de decisões de arquitetura |
| S — Structure | Estrutura de módulos/componentes descrita |
| O — Operations | Lista de tasks do documento de Tasks |
| N — Norms | Guidelines referenciados |

Registrar toda divergência encontrada como **divergência de canvas**.

### Fase 3 — Contradições entre artefatos

Verificar se PRD, TechSpec e Tasks se contradizem entre si (ex: RNF de performance no PRD não refletido em nenhuma decisão técnica; critério de aceite de task que conflita com regra de negócio do PRD). Registrar como **contradição**.

### Fase 4 — Geração do relatório

Salvar progressivamente em `docs/analyze/[feature]-analysis.md`, agrupado por tipo de achado:

```markdown
## Sumário
[contagem por tipo: N gaps, N divergências, N contradições]

## Gaps
- RF-XXX presente no PRD sem task correspondente
- TASK-XX.X sem RF de origem declarado

## Divergências
- Dimensão [X] do canvas diverge da TechSpec em [ponto]

## Contradições
- [artefato A] afirma [X], [artefato B] afirma [Y]
```

Salvar o arquivo após concluir cada fase (0 gaps ainda é resultado válido a persistir, não motivo para pular a seção).

### Fase 5 — Validação e handoff

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/analyze/validate-rules.json \
  --artifact docs/analyze/[feature]-analysis.md
```

Se houver gaps críticos (RF sem task): alertar o usuário antes de recomendar avançar para `/implement`.

## Artefatos

**Entrada:**
- `docs/prd/[feature]-prd.md` (obrigatório)
- `docs/techspec/[feature]-techspec.md` (obrigatório)
- `docs/tasks/[feature]-tasks.md` (obrigatório)
- `docs/spdd/[feature]-canvas.md` (opcional)

**Saída:**
- `docs/analyze/[feature]-analysis.md`

## Canvas

Esta skill **não atualiza** o canvas. Lê as dimensões preenchidas apenas para comparação com a TechSpec na Fase 2, sem assinar nenhuma dimensão como `_Atualizado por: /analyze_`.

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
- **Análise executada:** /analyze — [data]
- **Gaps:** [N] | **Divergências:** [N] | **Contradições:** [N]
- **Artefato:** docs/analyze/[feature]-analysis.md
```

Artifact Registry:
```
| analyze/[feature]-analysis.md | 1.0 | ok |
```
