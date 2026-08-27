# DDR-002 — Tipografia única (Roboto) + escala ancorada em 14px

**Status:** Accepted
**Data:** 2026-08-27
**Autor:** opencode (via /designer)

---

## Contexto

Ferramenta interna corporativa; prioriza legibilidade, familiaridade e performance de carregamento. Não há necessidade de identidade visual elaborada.

## Decisão

- **Família única:** Roboto (fallback system-ui, sans-serif) para heading e body.
- **Mono:** Roboto Mono (fallback monospace) para IDs, timestamps, código.
- **Pesos:** h1=700, h2=500, body/caption/code=400.
- **Escala:** caption/xs 12px · body/base 14px · h2 20px · h1 28px · code 13px.
- **Line-height:** 1.3 headings · 1.4 body/caption/code.

## Consequências

- Uma única fonte reduz overhead de carregamento e complexidade de composição.
- Trade-off aceito: sem diferenciação de família entre heading e body; hierarquia via peso/tamanho.
- Base 14px (não 16px) aumenta densidade de informação adequada a ferramenta de trabalho.

## Referências

- Design Brief: `docs/design/kanban-configuravel-design-brief.md` (Seção 2)