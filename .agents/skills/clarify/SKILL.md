---
name: clarify
description: Identifica e resolve ambiguidades no PRD ativo, fazendo perguntas direcionadas uma de cada vez e atualizando o documento incrementalmente. Use quando o PRD tem termos vagos, métricas ausentes, critérios de aceite imprecisos ou casos de borda não definidos que podem gerar retrabalho.
canvas-dimensions: [R]
input-artifacts:
  - memory/state.md
  - docs/prd/{{FEATURE}}-prd.md
output-artifacts:
  - docs/prd/{{FEATURE}}-prd.md
---

## Objetivo

Varrer o PRD em busca de ambiguidades — termos vagos, métricas não quantificadas, critérios de aceite imprecisos, casos de borda não cobertos — e resolvê-las com o usuário uma pergunta por vez, atualizando o PRD incrementalmente. Ao final, faz bump de versão MINOR do PRD e propaga o status `stale` aos artefatos downstream no Artifact Registry.

## Pré-condições

- `docs/prd/[feature]-prd.md` deve existir
- `memory/state.md` deve conter a entrada da feature no Artifact Registry

## Workflow

### Fase 0 — Leitura de contexto

1. `docs/prd/[feature]-prd.md` — ler o documento completo
2. `memory/state.md` — verificar versão atual do PRD e quais artefatos downstream (TechSpec, Tasks, Canvas) já existem no Artifact Registry

### Fase 1 — Identificação de ambiguidades

Varrer o PRD buscando:
- Termos vagos ("rápido", "simples", "adequado") sem métrica associada
- RNFs sem número/limiar quantificável
- Critérios de aceite sem Gherkin ou com passos incompletos
- Casos de borda mencionados na seção "Fora do Escopo" que na verdade deveriam estar cobertos
- RFs contraditórios entre si

Construir a lista de ambiguidades encontradas antes de perguntar — não interromper a leitura no meio.

### Fase 2 — Resolução (uma pergunta por vez)

**Regra crítica (princípio "one question at a time" do workspace):** apresentar uma ambiguidade por vez, nunca em lote. Para cada uma:

1. Citar o trecho ambíguo do PRD (referência de seção, não o texto completo se longo)
2. Perguntar a resposta específica que resolve a ambiguidade
3. Aguardar resposta do usuário
4. Atualizar o PRD imediatamente com a resposta antes de passar para a próxima ambiguidade

**Salvar o arquivo após cada resposta aplicada — não acumular mudanças para o final.**

### Fase 3 — Bump de versão e propagação de stale

Ao concluir todas as ambiguidades:

1. Incrementar a versão do PRD em MINOR (ex: `1.1` → `1.2`)
2. Atualizar `memory/state.md`:
   - Artifact Registry: nova versão do PRD, status `ok`
   - Para cada artefato downstream já gerado (TechSpec, Tasks, Canvas): marcar status `stale:prd@[nova-versão]`
3. Informar ao usuário quais artefatos downstream precisam ser regenerados

### Fase 4 — Validação e handoff

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/clarify/validate-rules.json \
  --artifact docs/prd/[feature]-prd.md
```

## Artefatos

**Entrada:**
- `memory/state.md` (obrigatório)
- `docs/prd/[feature]-prd.md` (obrigatório)

**Saída:**
- `docs/prd/[feature]-prd.md` — atualizado in-place, versão MINOR incrementada

## Canvas

Atualiza a dimensão:
- **R — Reasons:** quando a clarificação envolve o motivo de negócio ou objetivo original do RF, ajustar a dimensão R do canvas (se já existir) refletindo o motivo esclarecido, marcando `_Atualizado por: /clarify v1.0 — [data]_`
- Se a clarificação originar uma nova BDR, adicionar sua referência na linha `> Decisões:` da dimensão R

Se o canvas ainda não existir para a feature, pular esta etapa — ele será gerado por `/spdd-canvas` ou `/techspec` posteriormente já com o PRD clarificado.

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
- **Clarificação executada:** /clarify — [data]
- **Ambiguidades resolvidas:** [N]
- **PRD:** v[nova-versão]
- **Artefatos marcados stale:** [lista, ou "nenhum"]
```

Artifact Registry:
```
| docs/prd/[feature]-prd.md | [nova-versão] | ok |
| techspec/[feature]-techspec.md | [versão atual] | stale:prd@[nova-versão] |
```
