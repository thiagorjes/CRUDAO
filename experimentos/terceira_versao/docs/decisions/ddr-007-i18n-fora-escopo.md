# DDR-007 — i18n fora de escopo; pt-BR fixo sem chaves preparadas

**Status:** Accepted
**Data:** 2026-08-27
**Autor:** opencode (via /designer)

---

## Contexto

Ferramenta interna de equipe brasileira. Não há plano de internacionalização. Preparar chaves de extração (i18n-ready) adiciona overhead sem benefício imediato.

## Decisão

- UI em **português-BR fixo**.
- **Sem** extração de strings, chaves de tradução, nem infraestrutura de troca de idioma.
- Se i18n entrar no futuro, será retrabalho aceito (extração posterior).

## Consequências

- Código mais simples: strings diretamente nos componentes.
- Menor bundle, menos abstração.
- Decisão explícita evita "preparação prematura" que costuma não ser usada.

## Referências

- Design Brief: `docs/design/kanban-configuravel-design-brief.md` (Seção 7)