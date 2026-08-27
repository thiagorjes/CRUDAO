# DDR-001 — Paleta semântica Light-only (10 tokens canônicos + 1 opcional)

**Status:** Accepted
**Data:** 2026-08-27
**Autor:** opencode (via /designer)

---

## Contexto

O kanban-configuravel é uma ferramenta interna de uso diário da equipe de desenvolvimento. Precisa de uma paleta enxuta, semântica e sóbria, sem Dark mode nesta fase.

## Decisão

Adotar 10 tokens de cor canônicos + 1 opcional (`disabled`):

| Token | Hex | Uso |
|-------|-----|-----|
| primary | #0d6efd | Botões primários, links, foco, destaque de coluna válida no drag |
| success | #198754 | Sucesso de ações, impedimento resolvido, lead-time OK |
| error | #dc3545 | Impedimento ativo, erros, ações destrutivas (lixeira) |
| warning | #ffc107 | Reservado — sem uso definido |
| surface | #ffffff | Fundo de cards, modais, painéis |
| background | #f8f9fa | Fundo da página (board, dashboard, admin) |
| text-primary | #212529 | Títulos e corpo |
| text-secondary | #6c757d | Metadados, timestamps, labels secundários |
| border | #dee2e6 | Bordas de cards, divisórias, inputs |
| tipo-badge-bg | #e7f1ff | Fundo do badge de tipo no card |
| disabled | #adb5bd | (opcional) Estados desabilitados — se não usado, aplica-se `text-secondary`/`border` com opacidade reduzida |

Sem `primary-hover` nem `focus-ring` — derivados de `primary`. Dark mode fora de escopo.

## Consequências

- Paleta limitada a ~10 cores facilita consistência e manutenção.
- `disabled` como opcional evita inflar a paleta; fallback por opacidade é padrão CSS simples.
- Light-only simplifica implementação inicial; Dark mode pode ser adicionado como feature futura.

## Referências

- Design Brief: `docs/design/kanban-configuravel-design-brief.md` (Seção 2)