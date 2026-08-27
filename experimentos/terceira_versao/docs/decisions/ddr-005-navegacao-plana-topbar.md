# DDR-005 — Navegação plana com topbar + seletor de projeto global

**Status:** Accepted
**Data:** 2026-08-27
**Autor:** opencode (via /designer)

---

## Contexto

O projeto é o contexto principal de trabalho. Usuários trocam de projeto frequentemente. URLs aninhadas (/projects/:id/...) adicionam complexidade sem benefício para ferramenta interna.

## Decisão

- **Topbar enxuta** (sem sidebar) com: seletor de projeto + links "Dashboard" e "Configurações do projeto".
- **Rotas planas:** `/` (board), `/dashboard`, `/tarefas/:id`, `/admin`.
- **Contexto de projeto:** Global, implícito, persistido. Trocar no seletor de qualquer área muda o contexto de todas.
- Sem navegação "para dentro" de projeto pela URL.

## Consequências

- UX mais direta: uma troca de projeto atualiza board, dashboard e admin simultaneamente.
- URLs mais curtas e compartilháveis (`/tarefas/:id` funciona em qualquer projeto ativo).
- Simplifica roteamento e estado global.

## Referências

- Design Brief: `docs/design/kanban-configuravel-design-brief.md` (Seção 3)