---
name: spdd-canvas
description: Gera o REASONS Canvas completo a partir de PRD, TechSpec e Tasks para features que não passaram por /discovery, e é usada para atualização manual do canvas quando necessário. Use após aprovar a TechSpec quando o canvas ainda não existe ou está incompleto.
canvas-dimensions: [R, E, A, S, O, N]
input-artifacts:
  - memory/state.md
  - docs/prd/{{FEATURE}}-prd.md
  - docs/techspec/{{FEATURE}}-techspec.md
  - docs/tasks/{{FEATURE}}-tasks.md
output-artifacts:
  - docs/spdd/{{FEATURE}}-canvas.md
---

## Objetivo

Gerar (ou completar) o REASONS Canvas de uma feature a partir dos artefatos já existentes — PRD, TechSpec e, se disponível, Tasks. É o caminho de recuperação para features que não passaram por `/discovery` no início do pipeline, e também a skill usada para editar manualmente uma dimensão do canvas fora do fluxo automático de push de cada skill.

## Pré-condições

- `docs/prd/[feature]-prd.md` deve existir com status `ok`
- `docs/techspec/[feature]-techspec.md` deve existir com status `ok`
- `docs/tasks/[feature]-tasks.md` — opcional; se ausente, dimensão O fica vazia e o canvas permanece `DRAFT`
- Se `docs/spdd/[feature]-canvas.md` já existir: ler antes de sobrescrever, preservar dimensões já preenchidas por outras skills (nunca reverter ownership de uma dimensão mais recente)

## Workflow

### Fase 0 — Leitura de contexto

1. `memory/state.md` — confirmar status `ok` de PRD e TechSpec no Artifact Registry
2. `docs/prd/[feature]-prd.md` — objetivos de negócio, escopo, RFs, entidades de domínio
3. `docs/techspec/[feature]-techspec.md` — abordagem de solução, trade-offs, arquitetura, dependências
4. `docs/tasks/[feature]-tasks.md` se existir — lista de tasks ordenada
5. Se `docs/spdd/[feature]-canvas.md` já existir: ler para preservar dimensões preenchidas e evitar retrabalho

### Fase 1 — Geração progressiva, dimensão por dimensão

**Salvar o arquivo após cada dimensão preenchida — nunca aguardar o canvas completo.**

Preencher, nesta ordem, cada uma com heading `## [LETRA] — [Nome]`, linha de ownership `_Atualizado por: /spdd-canvas v1.0 — [data]_` e linha `> Decisões: [DRs relevantes ou —]`:

1. **R — Requirements:** objetivos de negócio e escopo IN/OUT, extraídos do PRD
2. **E — Entities:** entidades de domínio e diagrama, extraídos do PRD/data model da TechSpec
3. **A — Approach:** estratégia de solução e trade-offs, extraídos da TechSpec
4. **S — Structure:** arquitetura e dependências, extraídos da TechSpec
5. **O — Operations:** lista de tasks ordenada por dependência, extraída de Tasks (se ausente, deixar vazia e manter `DRAFT`)
6. **N — Norms:** padrões relevantes da feature, extraídos de `systems/[sistema]/guidelines/`

**Regra crítica:** nunca publicar o canvas com a dimensão **O** vazia como se estivesse `READY` — se Tasks não existir ainda, o canvas permanece `DRAFT` mesmo com as outras 6 dimensões preenchidas.

A dimensão **S — Safeguards** não é preenchida por esta skill — é ownership exclusivo de `/code-review`.

### Fase 2 — Verificação de completude

Ao final da geração:
- Se todas as 7 dimensões (R, E, A, S, O, N, S-safeguards) estiverem preenchidas e sem placeholder vazio → status `READY`
- Se qualquer uma estiver vazia (mais comumente O ou S-safeguards, ainda não geradas) → manter `DRAFT`

### Fase 3 — Validação

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/spdd-canvas/validate-rules.json \
  --artifact docs/spdd/[feature]-canvas.md
```

Isso executa também os `custom_steps`: `check_canvas_ownership.py` (toda dimensão tem linha de ownership) e `check_canvas_decisions.py` (toda dimensão tem linha `> Decisões:`).

### Fase 4 — Handoff

Atualizar `memory/state.md`:
- Artifact Registry: `spdd/[feature]-canvas.md | — | [ok se READY | draft se DRAFT]`
- Se canvas ficou `READY`: informar ao usuário que `/implement` já pode ser usado com contexto completo

## Artefatos

**Entrada:**
- `memory/state.md` (obrigatório)
- `docs/prd/[feature]-prd.md` (obrigatório — `ok`)
- `docs/techspec/[feature]-techspec.md` (obrigatório — `ok`)
- `docs/tasks/[feature]-tasks.md` (opcional)

**Saída:**
- `docs/spdd/[feature]-canvas.md` — canvas completo ou parcialmente preenchido

## Canvas

Esta skill atualiza as dimensões **R, E, A, S (Structure), O, N** do REASONS Canvas — todas exceto **S (Safeguards)**, que é ownership de `/code-review`.

Cada dimensão preenchida recebe ownership `_Atualizado por: /spdd-canvas v1.0 — [data]_`, mesmo quando o conteúdo é derivado de outro artefato (ex: R vem do PRD) — porque é esta skill, e não `/prd` diretamente, quem escreveu a dimensão no canvas nesta execução.

**Transição DRAFT → READY:** só ocorre quando as 7 dimensões estiverem preenchidas, incluindo O (requer Tasks já gerada).

## Handoff

Ao concluir, registrar em `memory/state.md`:

```markdown
- **Canvas gerado/atualizado:** docs/spdd/[feature]-canvas.md — [data]
- **Dimensões preenchidas:** [lista, ex: R, E, A, S, N — O pendente]
- **Status do canvas:** [DRAFT | READY]
- **Próximo comando:** [/tasks (se O pendente) | /implement (se READY)]
```

Artifact Registry:
```
| spdd/[feature]-canvas.md | — | [ok | draft] |
```
