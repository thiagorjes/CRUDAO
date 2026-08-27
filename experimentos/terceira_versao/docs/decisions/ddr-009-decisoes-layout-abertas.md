# DDR-009 — 6 decisões de layout abertas para exploração no protótipo (12 variações)

**Status:** Accepted
**Data:** 2026-08-27
**Autor:** opencode (via /designer)

---

## Contexto

O modelo de interação está fechado (drag-drop + menu, modal/toast/skeleton/spinner). Restam decisões de densidade e arranjo visual que o protótipo deve explorar para validação com a equipe.

## Decisão

Prototipar **2 variações** para cada uma das 6 decisões abertas:

| Decisão | Opções | Tela(s) |
|---------|--------|---------|
| Densidade do card no board | Compacto / Confortável | S01 |
| Apresentação das raias | Sempre expandidas / Recolhíveis (padrão recolhido se muitas) | S01 |
| Ícone de exclusão no card | Sempre visível / No hover | S01 |
| Navegação interna do /admin | Abas horizontais / Lista lateral | S03 |
| Layout do Dashboard | Gráfico + tabela empilhados / Lado a lado | S02 |
| Modal Criar Card | Só essencial (título) com resto recolhido / Formulário completo visível | S05 |

**Total:** 12 variações de protótipo.
**Demais telas (S04, S06, S07, S08, S09):** 1 variação cada — composições diretas.

## Consequências

- Protótipo navegável permite validação real com a equipe antes do /techspec.
- Decisões baseadas em uso real, não em preferência abstrata.
- Limita escopo: apenas 6 decisões abertas, não redesign completo.

## Referências

- Design Brief: `docs/design/kanban-configuravel-design-brief.md` (Seção 8)