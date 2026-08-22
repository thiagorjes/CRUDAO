---
name: designer
description: Conduz entrevista de descoberta de design para definir guidelines de UX/UI e gerar um design brief para prototipagem. Atualiza dimensão E do canvas com entidades visuais/UX. Use no início de features com interface visual, antes do handoff para prototipagem.
canvas-dimensions: [E]
input-artifacts:
  - memory/state.md
  - docs/prd/{{FEATURE}}-prd.md
output-artifacts:
  - docs/design/{{FEATURE}}-design-brief.md
---

## Objetivo

Levantar, através de entrevista estruturada, os tokens visuais, componentes e padrões de interação necessários para a feature, produzindo um Design Brief pronto para prototipagem. Cada decisão de design system relevante gera um Decision Record do tipo DDR.

## Pré-condições

- `docs/prd/[feature]-prd.md` deve existir com status `ok` no Artifact Registry
- `memory/state.md` com a feature registrada

## Workflow

### Fase 0 — Leitura de contexto

1. `docs/prd/[feature]-prd.md` — objetivo de negócio, público-alvo, jornadas descritas
2. `memory/state.md` — confirmar versão do PRD no Artifact Registry
3. `docs/spdd/[feature]-canvas.md`, se existir — ler dimensão E já preenchida por `/prd`/`/techspec` para não duplicar entidades de domínio

### Fase 1 — Levantamento de tokens visuais (uma pergunta por vez)

Perguntar, **uma de cada vez**, absorvendo o que já estiver decidido em guidelines/PRD:

1. Paleta de cores (primária, secundária, fundo, superfície, erro, sucesso, texto) — ou "usar design system existente: [nome]"
2. Tipografia (fonte heading, fonte body, fonte mono, escala de tamanhos)
3. Grid e breakpoints (mobile/tablet/desktop)
4. Escala de espaçamento (base 4px ou 8px)

Salvar respostas incrementalmente no Design Brief à medida que forem obtidas.

### Fase 2 — Componentes

Para cada tela/fluxo relevante do PRD:
1. Identificar componentes necessários (ex: botão, card, modal, formulário)
2. Para cada componente: variantes, estados (default/hover/active/disabled/loading/error)
3. Salvar seção de Componentes do Design Brief

### Fase 3 — Padrões de interação

1. Levantar padrões de feedback (sucesso, erro, loading) e transições/animações
2. Confirmar requisitos de acessibilidade (contraste WCAG AA, foco visível, leitores de tela, tamanho mínimo de toque)
3. Salvar seções de Padrões de Interação e Acessibilidade

### Fase 4 — Decision Records de Design

Para cada decisão de design system relevante tomada nas Fases 1-3 (ex: escolha de paleta, escolha de grid, padrão de componente não trivial):
1. Verificar próximo número de sequência DDR no índice de `memory/constitution.md`
2. Criar `docs/decisions/ddr-[NNN]-[slug].md` a partir do template de Decision Record
3. Adicionar ao índice de DDRs em `memory/constitution.md`
4. Referenciar o DDR na seção 8 do Design Brief

### Fase 5 — Atualização do Canvas (dimensão E)

Atualizar dimensão **E — Entities** do canvas `docs/spdd/[feature]-canvas.md`, complementando (não substituindo) as entidades de domínio já registradas por `/prd`/`/techspec` com as entidades de UX/UI:
- Componentes principais e seus tokens
- `_Atualizado por: /designer v1.0 — [data]_`
- `> Decisões: DDR-[NNN], ...`

Salvar o canvas após a atualização.

### Fase 6 — Validação e Handoff

1. Validar o Design Brief:
   ```
   python .agents/scripts/validate.py --mode output \
     --rules .agents/skills/designer/validate-rules.json \
     --artifact docs/design/[feature]-design-brief.md
   ```
2. Atualizar `memory/state.md`:
   - Artifact Registry: `docs/design/[feature]-design-brief.md | 1.0 | ok`
3. Sugerir próximo passo: prototipagem (fora do pipeline SDD) ou `/techspec` se ainda não executado

## Artefatos

**Entrada:**
- `docs/prd/[feature]-prd.md` (obrigatório)
- `memory/state.md` (obrigatório)
- `docs/spdd/[feature]-canvas.md` (opcional — se já existir)

**Saída:**
- `docs/design/[feature]-design-brief.md`
- `docs/decisions/ddr-[NNN]-[slug].md` (um por decisão de design system)

## Canvas

Esta skill atualiza:
- **E — Entities:** entidades de UX/UI (componentes, tokens), complementando as entidades de domínio
- Referências a DDRs criadas nesta fase: `> Decisões: DDR-001, ...` (ou `> Decisões: —` se nenhuma)

## Handoff

Ao concluir, registrar em `memory/state.md` (seção da feature ativa):

```markdown
- **Etapa concluída:** /designer (v1.0) — [data]
- **Artefato:** docs/design/[feature]-design-brief.md
- **DDRs criados:** DDR-[NNN], ...
```
