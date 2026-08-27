# DDR-006 — WCAG AA não obrigatório; acessibilidade mínima via componentes nativos

**Status:** Accepted
**Data:** 2026-08-27
**Autor:** opencode (via /designer)

---

## Contexto

Sistema interno / validação de conceito. Equipe conhecida, ambiente controlado. Conformidade formal WCAG AA adicionaria custo significativo sem ROI claro nesta fase.

## Decisão

- WCAG AA **não é requisito** desta fase.
- **Navegação por teclado:** apenas o que componentes nativos oferecem (inputs, botões, modais). Board **não exige** malha Tab/Setas entre colunas/cards.
- **Alternativa ao drag-drop:** menu de movimentação no card (acessível por teclado nativo).
- **Leitor de tela:** ARIA limitado ao default dos componentes. Sem `aria-grabbed`/`aria-dropeffect` nem regiões roladas manualmente.
- Registrado como **dívida técnica** a revisitar se público se ampliar.

## Consequências

- Reduz escopo de implementação e testes de acessibilidade.
- Drag-drop permanece como interação principal; menu no card garante alternativa básica.
- Se houver necessidade regulatória ou de inclusão futura, será trabalho dedicado.

## Referências

- Design Brief: `docs/design/kanban-configuravel-design-brief.md` (Seção 7)