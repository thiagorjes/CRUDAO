# Design Brief — Criação e gerenciamento de cards no board
_Versão: 1.0 | Data: 2026-08-24 | Designer: Thiago Goncalves Cavalcante_
_PRD: docs/prd/criacao-card-board-prd.md_

---

## 1. Contexto e Objetivo

**Feature:** criacao-card-board
**Objetivo de UX:** permitir criar e excluir cards diretamente pelo board, sem depender de chamada direta à API, mantendo consistência total com os padrões visuais já estabelecidos em `kanban-configuravel`.
**Público-alvo:** usuários com `tarefa:gerenciar` no projeto (`project_admin`, `dev`-tier).

---

## 2. Tokens de Cor

Design system detectado e reaproveitado integralmente, sem nenhum token novo — ver `docs/design/kanban-configuravel-design-brief.md` §2 (`--color-primary`, `--color-error`, `--color-success`, `--color-surface`, `--color-border` etc.) e `docs/design/kanban-configuravel/design-tokens.json` para os valores exatos.

---

## 3. Tipografia

Reaproveitada integralmente de `docs/design/kanban-configuravel-design-brief.md` §3 (Roboto — heading/body/caption). Nenhum token novo.

---

## 4. Espaçamento e Grid

Reaproveitado integralmente de `docs/design/kanban-configuravel-design-brief.md` §4 (escala base 8px, breakpoint único desktop ≥1024px). Nenhuma tela nova (rota) — a feature adiciona elementos ao Board principal (`/`, `frontend/src/components/board/BoardApp.tsx`) já existente.

---

## 5. Componentes

Reaproveita os componentes **Modal de Confirmação** e **Toast/Snackbar** já existentes no inventário de `docs/design/kanban-configuravel-design-brief.md` §5, sem variantes novas.

**Novos elementos de composição (não são componentes de design system, apenas arranjo desta feature):**
- **Botão "Novo card":** posição fixa no header do board; visível só a quem tem `tarefa:gerenciar` no projeto.
- **Modal "Novo card":** usa o padrão visual de Modal existente; campos título (obrigatório)/descrição/tipo/demais atributos; estados idle, erro de validação (título vazio), salvando (loading), sucesso (toast).
- **Ícone de lixeira no Card de Tarefa:** sempre visível (não só hover), gated pela mesma permissão + toggle `devPodeExcluirTarefa` para `dev`-tier.

### Inventário de Telas

| ID | Nome | RF(s) | Persona | Rota |
|----|------|-------|---------|------|
| TL-01 | Board com botão "Novo card" | RF-001 | Usuário com `tarefa:gerenciar` | `/` (existente) |
| TL-02 | Modal "Novo card" | RF-001 | Usuário com `tarefa:gerenciar` | overlay sobre `/` |
| TL-03 | Card com ícone de exclusão | RF-002 | Usuário com `tarefa:gerenciar` | `/` (existente) |
| TL-04 | Modal de confirmação de exclusão | RF-002 | Usuário com `tarefa:gerenciar` | overlay sobre `/` |

### Estados por Tela

| Tela | Idle | Loading | Preenchido | Erro | Sucesso | Vazio |
|------|------|---------|------------|------|---------|-------|
| TL-01 Board (botão) | ✅ obrigatório | — | — | — | — | — |
| TL-02 Modal Novo card | ✅ obrigatório | ✅ obrigatório (salvando) | ✅ obrigatório | ✅ obrigatório (título vazio) | ✅ obrigatório (toast) | — |
| TL-03 Card (ícone lixeira) | ✅ obrigatório | — | — | — | — | — |
| TL-04 Modal confirmação exclusão | ✅ obrigatório | ✅ obrigatório (botão Excluir) | — | — | — | — |

---

## 6. Padrões de Interação

**UC-001 — Criar card (happy path):**
1. Board (TL-01) → clique em "Novo card" (posição: header do board, sempre visível a quem tem `tarefa:gerenciar`)
2. Abre Modal "Novo card" (TL-02) — estado idle
3. Preenche título (obrigatório), descrição, tipo, demais atributos
4. Salva → estado "salvando" (loading no botão) → sucesso → modal fecha, toast de sucesso, card aparece na coluna 0 / primeira raia sem reload

**Fluxo de erro — título vazio:**
1. Modal (TL-02) → clica Salvar sem preencher título
2. Estado de erro de validação: campo título sinalizado, envio bloqueado, sem chamada à API

**UC-002 — Excluir card (happy path):**
1. Card (TL-03) → clique no ícone de lixeira (sempre visível, sem depender de hover)
2. Abre Modal de confirmação (TL-04)
3. Confirma → botão "Excluir" em estado loading → card removido do board, modal fecha

**Fluxo alternativo — cancelar exclusão:**
1. Modal (TL-04) → clica "Cancelar" → modal fecha, nenhuma alteração

---

## 7. Acessibilidade

Herdado de `docs/design/kanban-configuravel-design-brief.md` §7 — sem requisitos obrigatórios nesta versão (sistema interno, desktop-only, DDR-003). Idioma único (pt_BR) — sem requisitos adicionais de i18n.

**Decisões em aberto:** nenhuma — decisões de layout já fechadas na entrevista (modal, botão no header, ícone de lixeira sempre visível). Decisão técnica não resolvida (evento `TAREFA_EXCLUIDA`) fica registrada no PRD §7 para `/techspec`, não afeta o protótipo visual.

**Escopo do protótipo:**
- Telas: TL-01 (recorte do board com botão), TL-02 (modal criação, 4 estados), TL-03 (recorte do card com ícone), TL-04 (modal confirmação, 2 estados)
- Variações de layout: nenhuma — 1 variação única por decisão já fechada
- Reaproveitar diretamente os componentes/tokens de `docs/design/kanban-configuravel/design-tokens.json` (protótipo desta feature não deve redefinir tokens)

---

## 8. Decision Records de Design (DDR)

Nenhum DDR novo — decisões desta feature são de composição de componentes já existentes, sem novo padrão de design system.

---

## 9. Referências

- Design system base: `docs/design/kanban-configuravel-design-brief.md` (DDR-001/002/003)
- Tokens: `docs/design/kanban-configuravel/design-tokens.json`
- Protótipo de referência: `docs/design/prototypes/kanban-configuravel/` (Artifact https://claude.ai/code/artifact/a7612319-88d0-434d-9729-64d3d1604c6c)
- Protótipo desta feature: `docs/design/criacao-card-board/prototypes/CriacaoExclusaoCard.html`
- Screen map: `docs/design/criacao-card-board/screen-map.md`
