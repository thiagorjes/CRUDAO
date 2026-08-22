---
name: checklist
description: Gera checklists de qualidade para validar o quão bem os requisitos estão escritos no PRD e na TechSpec, funcionando como testes unitários dos requisitos. Use ao verificar se requisitos estão completos, claros e mensuráveis antes de avançar para TechSpec ou implementação.
canvas-dimensions: []
input-artifacts:
  - memory/state.md
  - docs/prd/{{FEATURE}}-prd.md
output-artifacts:
  - docs/checklists/{{FEATURE}}-{{TIPO}}.md
---

## Objetivo

Aplicar um checklist de qualidade a um artefato (PRD ou TechSpec), tratando cada requisito como um caso a validar — não a implementação em si, mas a qualidade da especificação. Gera `docs/checklists/[feature]-[tipo].md` diferenciando itens críticos (bloqueiam a próxima etapa do pipeline) de itens não-críticos (melhorias sugeridas).

## Pré-condições

- `docs/prd/[feature]-prd.md` deve existir (mínimo)
- `docs/techspec/[feature]-techspec.md` — necessário apenas se o checklist for do tipo `techspec`
- `memory/state.md` com a feature registrada no Artifact Registry

## Workflow

### Fase 0 — Leitura de contexto

1. Perguntar (se não informado nos argumentos) qual artefato validar: PRD ou TechSpec
2. Ler o artefato-alvo por completo
3. `memory/state.md` — confirmar versão do artefato no Artifact Registry

### Fase 1 — Aplicação do checklist de qualidade

Para cada requisito (RF/RNF no PRD, decisão técnica na TechSpec), verificar:

| Critério | Pergunta |
|---|---|
| Completude | O requisito descreve o comportamento esperado por completo, sem lacunas? |
| Clareza | Está livre de termos vagos ("rápido", "adequado", "amigável")? |
| Mensurabilidade | RNFs têm limiar numérico? RFs têm critério de aceite testável (Gherkin)? |
| Consistência | Não contradiz outro requisito do mesmo documento? |
| Rastreabilidade | Tem ID único e referência a fonte (ex: RF-XXX)? |

Cada falha encontrada vira um item do checklist, com ID sequencial `CHK-NNN`.

### Fase 2 — Classificação crítico vs. não-crítico

Classificar cada item:
- **Crítico:** bloqueia a próxima etapa do pipeline (ex: RNF sem métrica impede `/techspec` de dimensionar a solução)
- **Não-crítico:** melhoria de qualidade que não impede avançar, mas deveria ser corrigida

### Fase 3 — Geração do checklist

Salvar progressivamente em `docs/checklists/[feature]-[tipo].md`:

```markdown
## Sumário
[N itens críticos, N itens não-críticos]

## Itens Críticos
- CHK-001 — [requisito] — [problema] — bloqueia: [etapa]

## Itens Não-Críticos
- CHK-002 — [requisito] — [sugestão de melhoria]
```

Salvar o arquivo assim que a Fase 1 identificar os primeiros itens — não aguardar terminar a varredura completa do documento.

### Fase 4 — Validação e handoff

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/checklist/validate-rules.json \
  --artifact docs/checklists/[feature]-[tipo].md
```

Se houver itens críticos: alertar o usuário que a próxima etapa do pipeline está bloqueada até resolução.

## Artefatos

**Entrada:**
- `memory/state.md` (obrigatório)
- `docs/prd/[feature]-prd.md` ou `docs/techspec/[feature]-techspec.md` (o artefato-alvo)

**Saída:**
- `docs/checklists/[feature]-[tipo].md`

## Canvas

Esta skill **não atualiza** o canvas — atua sobre a qualidade de PRD/TechSpec, artefatos anteriores ao canvas no pipeline.

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
- **Checklist executado:** /checklist [tipo] — [data]
- **Itens críticos:** [N] | **Itens não-críticos:** [N]
- **Artefato:** docs/checklists/[feature]-[tipo].md
```

Artifact Registry:
```
| checklists/[feature]-[tipo].md | 1.0 | ok |
```
