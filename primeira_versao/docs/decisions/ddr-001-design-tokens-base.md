---
id: DDR-001
type: DDR
status: accepted
date: 2026-08-22
supersedes: —
superseded-by: —
---

# DDR-001 — Tokens base de design: cores, tipografia e espaçamento

## Decisão

Tema sóbrio e corporativo. Cor primária azul `#0d6efd`, cor de sucesso/secundária verde `#198754`, paleta total com no máximo 10 cores. Tipografia única: Roboto (heading e body). Espaçamento em base 8px. Grid responsivo a partir de 1024px (desktop-only).

## Motivação

Sistema de uso interno da equipe, sem necessidade de identidade visual elaborada — prioriza legibilidade e familiaridade corporativa.

**Problema que resolve:**
Definir uma base visual consistente antes de detalhar componentes, evitando decisões ad-hoc durante a implementação do frontend.

**Restrições consideradas:**
- RNF-005 do PRD: responsivo apenas para desktop.
- Sistema interno/POC — acessibilidade formal (WCAG) não é obrigatória nesta fase (ver DDR-003).

## Consequências

**Positivas:**
- Paleta enxuta (≤10 cores) simplifica manutenção do design system.
- Fonte única (Roboto) reduz overhead de carregamento e complexidade de composição tipográfica.

**Negativas / trade-offs:**
- Sem diferenciação tipográfica entre heading/body — hierarquia visual depende de peso/tamanho, não de família de fonte.

**Downstream afetado:**
- Design Brief e implementação de componentes no frontend Next.js.

## Alternativas Consideradas

### Alternativa 1 — Orbitron para headings + Roboto para body
**Descartada porque:** usuário optou por simplicidade com fonte única, mais alinhada ao tom corporativo do que o estilo futurista da Orbitron.

### Alternativa 2 — Suporte a tablet (breakpoint ≥768px)
**Descartada porque:** escopo confirmado é desktop-only a partir de 1024px.
