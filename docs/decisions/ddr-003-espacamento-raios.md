# DDR-003 — Espaçamento base 8px, raios 4/6/8/50%

**Status:** Accepted
**Data:** 2026-08-27
**Autor:** opencode (via /designer)

---

## Contexto

Sistema de espaçamento consistente para layout denso mas legível, adequado a board kanban com muitos cards visíveis.

## Decisão

- **Base:** 8px com meio-passo de 4px.
- **Escala:** xs 4 · sm 8 · md 16 · lg 24 · xl 32.
- **Raios:** sm 4 · md 6 · lg 8 · pill 50%.

## Consequências

- Grade de 8px alinha com práticas modernas (Material, Bootstrap, Tailwind).
- Meio-passo de 4px permite ajustes finos sem quebrar a grade.
- Raios limitados a 4 valores mantêm consistência visual.

## Referências

- Design Brief: `docs/design/kanban-configuravel-design-brief.md` (Seção 2)