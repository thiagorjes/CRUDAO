# DDR-004 — Desktop-only ≥ 1024px, sem responsividade mobile/tablet

**Status:** Accepted
**Data:** 2026-08-27
**Autor:** opencode (via /designer)

---

## Contexto

Ferramenta interna usada em desktop (monitores de desenvolvedores). Mobile/tablet não são cenários de uso nesta fase.

## Decisão

- Breakpoint único: ≥ 1024px (desktop).
- Sem layout adaptativo para < 1024px.
- Responsividade mobile/tablet fora de escopo — registrado como limitação conhecida.

## Consequências

- Simplifica CSS e testes significativamente.
- Permite layouts de board com colunas fixas, densidade alta, drag-drop nativo.
- Se público se ampliar no futuro, responsividade será retrabalho aceito.

## Referências

- Design Brief: `docs/design/kanban-configuravel-design-brief.md` (Seções 2, 3, 7)