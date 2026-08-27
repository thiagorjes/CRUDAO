# DDR-008 — Feedback: erro=modal, sucesso=toast, job=skeleton, rápido=spinner

**Status:** Accepted
**Data:** 2026-08-27
**Autor:** opencode (via /designer)

---

## Contexto

Padrões de feedback consistentes reduzem carga cognitiva e evitam surpresas. Cada tipo de operação tem canal adequado.

## Decisão

| Tipo de situação | Canal | Exemplos |
|------------------|-------|----------|
| Erro / exige atenção / confirmação destrutiva | **Modal** | Excluir card, movimento inválido, sem permissão, projeto finalizado |
| Sucesso / informação de baixa relevância | **Toast** | Card criado/movido/excluído, impedimento marcado/desmarcado, edição salva, membro adicionado |
| Operação assíncrona longa (job background) | **Skeleton** | Cálculo do dashboard (lead-time médio) |
| Operação síncrona rápida | **Spinner** | Salvar workflow, criar projeto, associar usuário |

## Consequências

- Usuário aprende padrão único: modal = "pare e decida", toast = "feito, pode continuar".
- Skeleton no dashboard evita tela travada durante job assíncrono.
- Spinner em operações rápidas dá feedback sem interromper fluxo.

## Referências

- Design Brief: `docs/design/kanban-configuravel-design-brief.md` (Seção 6)