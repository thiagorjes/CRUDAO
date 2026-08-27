# Design Brief — kanban-configuravel
_Versão: 1.0 | Status: Draft | Data: 2026-08-27 | Autor: opencode_

---

## 1. Contexto e objetivo

Este brief deriva do PRD **kanban-configuravel** (v1.0, 18 RFs Must Have, 5 RNFs, 17 regras de negócio, 5 casos de uso). O produto é um sistema de kanban com etapas configuráveis por projeto, onde desenvolvedores atualizam status e sinalizam impedimentos em tempo real, com notificações automáticas e lead-time visível por etapa e agregado em dashboard.

**Público-alvo:** Equipe de Desenvolvimento (devs e liderança técnica) — execução e acompanhamento diário. Persona secundária: gestores de outros times que precisam de visibilidade de progresso, impedimentos e lead-time sem atualizar dados.

**Escopo visual:** 9 telas/views (S01–S09) cobrindo board, dashboard, painel de administração, detalhe de tarefa e 5 modais.

---

## 2. Identidade visual

### Tom do produto
Sério/corporativo com inclinação minimalista. Ferramenta de trabalho onde a equipe passa o dia. Cores sóbrias e semânticas, tipografia formal neutra, muito espaço, pouca ornamentação. Foco em eficiência: drag-and-drop com destaque do alvo, caminho alternativo por menu, feedback imediato e discreto.

### Paleta (Light only — 10 tokens canônicos + 1 opcional)

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

> **Nota:** Sem `primary-hover` nem `focus-ring` — derivados de `primary`. Dark mode fora de escopo.

### Tipografia e grid

| Item | Decisão |
|------|---------|
| Fonte heading | Roboto (fallback system-ui, sans-serif) — peso 700 (h1) / 500 (h2) |
| Fonte body | Roboto (peso 400) |
| Fonte mono | Roboto Mono (fallback monospace) — IDs, timestamps, código |
| Escala de tamanhos | caption/xs 12px · body/base 14px · heading 2 20px · heading 1 28px · code 13px |
| Line-height | 1.3 headings · 1.4 body/caption/code |
| Base de espaçamento | 8px (meio-passo 4px) — xs 4, sm 8, md 16, lg 24, xl 32 |
| Raios | sm 4 · md 6 · lg 8 · pill 50% |
| Breakpoints | Desktop-only ≥ 1024px. Sem responsividade abaixo. |

---

## 3. Navegação e layout

**Padrão:** Topbar enxuta, sem sidebar. Seletor de projeto no topo (contexto global implícito, persistido, compartilhado entre áreas). Links diretos: "Dashboard" e "Configurações do projeto".

**Rotas principais:**

| Rota | Descrição | Acesso |
|------|-----------|--------|
| `/` | Board do projeto — tela principal | Qualquer membro do projeto |
| `/dashboard` | Dashboard de gestão — lead-time médio por etapa/impedimento | Permissão de visualizar dashboard |
| `/tarefas/:id` | Detalhe da tarefa — tempo, impedimentos, observadores, atribuição, edição, histórico | Membro do projeto |
| `/admin` | Painel de Administração — abas Projetos, Workflows, Colunas, Raias, Membros, Toggles, Papéis | Admin global / Project Admin |

**Contexto de projeto:** Global e implícito. Trocar o projeto no seletor de qualquer área muda o contexto de todas. Não se navega "para dentro" de um projeto pela URL.

---

## 4. Inventário de telas

| ID | Tela / View | RF(s) | Persona(s) | Rota | Origem → Destino |
|----|-------------|-------|------------|------|------------------|
| S01 | Board do projeto — colunas, cards, drag/drop, impedimento, criar/excluir | RF-001, RF-002, RF-004, RF-012, RF-018, RF-019 | dev, product_owner, project_admin | `/` | Topbar → S02, S03; clique card → S04; "Novo card" → S05; lixeira → S06; finalizar → S07 |
| S02 | Dashboard de gestão — lead-time médio, impedimento, seletor período | RF-007 | gestor, project_admin, admin global | `/dashboard` | Topbar → S01, S03 |
| S03 | Painel de Administração — abas Projetos, Workflows, Colunas, Raias, Membros, Toggles, Papéis | RF-008, RF-009, RF-010, RF-011, RF-013, RF-015, RF-016 | admin global, project_admin | `/admin` | Topbar "Configurações" → S03; abas → S08, S09 |
| S04 | Detalhe da Tarefa — tempo por etapa, impedimentos, observadores, atribuição, edição, histórico | RF-003, RF-004, RF-005, RF-006, RF-017 | dev, product_owner, gestor | `/tarefas/:id` | S01 (clique card) → S04; voltar → S01 |
| S05 | Modal Criar Card — título, descrição, tipo; etapa/raia automáticas | RF-018 (RF-003) | dev, product_owner, project_admin | overlay em `/` | S01 (botão "Novo card") → S05; salvar → S01 |
| S06 | Modal Confirmar Exclusão de Card | RF-019 | product_owner, project_admin, dev (se toggle) | overlay em `/` | S01 (lixeira) → S06; confirmar → S01 |
| S07 | Modal Finalizar / Desfinalizar — escolha etapa destino | RF-012 | product_owner, project_admin, admin global | overlay em `/` | S01 (ação no card) → S07 → S01 |
| S08 | Configuração Workflow / Colunas / Transições | RF-002, RF-009, RF-010 | project_admin, admin global | abas Workflows/Colunas em `/admin` | S03 → S08 → S03 |
| S09 | Gestão Membros / Toggles / Papéis do Projeto | RF-013, RF-015, RF-016 | project_admin (Membros/Toggles), admin global (Papéis) | abas Membros/Toggles/Papéis em `/admin` | S03 → S09 → S03 |

---

## 5. Fluxos de navegação

### Happy path
- S01 → arrasta card → S01 (atualização tempo real para todos)
- S01 → S05 → S01 (criar card)
- S01 → S06 → S01 (excluir card)
- S01 → S04 → S01 (detalhe tarefa)
- S03 → S08/S09 → S03 (configuração)

### Fluxo de erro crítico
- Movimento sem transição válida → operação recusada pelo backend, feedback visual no board sem alterar o card
- Projeto finalizado → board em somente-leitura, ações de escrita bloqueadas
- Criar card sem título → validação local, sem chamada ao backend
- Excluir/criar card com evento atrasado ou id já presente → operação idempotente, sem duplicar nem re-remover

---

## 6. Estados por tela (obrigatórios no protótipo)

| Tela | idle | loading | preenchido | erro | sucesso | vazio |
|------|------|---------|------------|------|---------|-------|
| S01 Board | ✓ | ✓ (skeleton) | ✓ | ✓ | ✓ (toast: mover, impedimento, criar, excluir) | ✓ (sem workflow) |
| S02 Dashboard | ✓ | ✓ (skeleton, job bg) | ✓ | ✓ | — | ✓ (sem dados no período) |
| S03 Admin | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ (sem projetos — inclui criar primeiro) |
| S04 Detalhe Tarefa | ✓ | ✓ | ✓ | ✓ | ✓ (toast: editar, atribuir, impedimento) | ✓ parcial (sem impedimentos, observadores, histórico) |
| S05 Modal Criar Card | ✓ | ✓ (salvando) | — | ✓ (validação título) | ✓ | — |
| S06 Modal Excluir Card | ✓ | ✓ (excluindo) | — | ✓ | ✓ | — |
| S07 Modal Finalizar/Desfinalizar | ✓ | ✓ | — | ✓ (sem permissão, projeto finalizado) | ✓ | — |
| S08 Config Workflow/Colunas | ✓ | ✓ | ✓ | ✓ (excluir etapa com tarefas) | ✓ | ✓ |
| S09 Gestão Membros/Toggles/Papéis | ✓ | ✓ | ✓ | ✓ (remover último papel admin) | ✓ | ✓ |

**Padrões de feedback:**
- Erro/situações que exigem atenção → modal de confirmação
- Sucesso/baixa relevância → toast
- Operação assíncrona longa (job dashboard) → skeleton
- Operação síncrona rápida → spinner

---

## 7. Acessibilidade e internacionalização

| Item | Decisão |
|------|---------|
| Plataformas-alvo | Desktop (navegador moderno) |
| Breakpoint prioritário | ≥ 1024px, desktop-only |
| Acessibilidade (nível) | WCAG AA **não obrigatório** — sistema interno / validação de conceito. Dívida técnica registrada. |
| Navegação por teclado | Apenas o que componentes nativos oferecem (inputs, botões, modais). Board não exige malha Tab/Setas. Alternativa ao drag-drop: menu no card. |
| Leitor de tela | Sem meta de conformidade. ARIA limitado ao default dos componentes. Sem `aria-grabbed`/`aria-dropeffect` nem regiões roladas manualmente. |
| i18n | Fora de escopo. UI em pt-BR fixo. Sem preparação de chaves para extração futura. |

---

## 8. Decisões em aberto (para o protótipo explorar)

| Decisão | Opções | Impacto | Variações |
|---------|--------|---------|-----------|
| Densidade do card no board | Compacto (título + responsável + tempo) / Confortável (+ badge tipo, menu sempre visível) | Cards por coluna vs. legibilidade | 2 em S01 |
| Apresentação das raias | Sempre expandidas / Recolhíveis com padrão recolhido quando muitas | Espaço vertical com vários devs | 2 em S01 |
| Ícone de exclusão no card | Sempre visível / Aparece no hover | Descobribilidade vs. ruído/acidente | 2 em S01 |
| Navegação interna do /admin | Abas horizontais / Lista lateral | Escala conforme nº de seções cresce | 2 em S03 |
| Layout do Dashboard | Gráfico e tabela empilhados / Lado a lado | Leitura rápida vs. valores precisos na mesma dobra | 2 em S02 |
| Modal Criar Card | Só essencial (título) com resto opcional recolhido / Formulário completo visível | Velocidade vs. completude ao nascer | 2 em S05 |

**Demais telas (S04, S06, S07, S08, S09):** 1 variação cada — composições diretas de componentes definidos.

---

## 9. Escopo do protótipo

- **Telas:** S01–S09 (9 telas/views)
- **Variações:** 12 no total (2×6 decisões abertas + 1×3 telas fixas)
- **Estados obrigatórios:** Conforme matriz da Seção 6
- **Tokens:** 10 cores canônicas + 1 opcional (`disabled`) + tipografia + espaçamento + raios (Seção 2)
- **Navegação:** Topbar com seletor de projeto, links Dashboard/Configurações

---

## 10. Decision Records de Design (DDRs)

| DDR | Título | Decisão |
|-----|--------|---------|
| DDR-001 | Paleta semântica Light-only (10 tokens canônicos + 1 opcional) | Aprovada conforme Seção 2 |
| DDR-002 | Tipografia única (Roboto) + escala ancorada em 14px | Aprovada conforme Seção 2 |
| DDR-003 | Espaçamento base 8px, raios 4/6/8/50% | Aprovada conforme Seção 2 |
| DDR-004 | Desktop-only ≥ 1024px, sem responsividade mobile/tablet | Aprovada conforme Seção 3 |
| DDR-005 | Navegação plana com topbar + seletor de projeto global | Aprovada conforme Seção 3 |
| DDR-006 | WCAG AA não obrigatório; acessibilidade mínima via componentes nativos | Aprovada conforme Seção 7 |
| DDR-007 | i18n fora de escopo; pt-BR fixo sem chaves preparadas | Aprovada conforme Seção 7 |
| DDR-008 | Feedback: erro=modal, sucesso=toast, job=skeleton, rápido=spinner | Aprovada conforme Seção 6 |
| DDR-009 | 6 decisões de layout abertas para exploração no protótipo (12 variações) | Aprovada conforme Seção 8 |

> DDRs serão criados em `docs/decisions/ddr-[NNN]-[slug].md` e indexados em `memory/constitution.md` na Fase 5.

---

## Próximos passos

1. **Confirmação do brief** (Fase 4) — ajustes se necessário
2. **Criação dos DDRs** (Fase 5)
3. **Atualização do Canvas dimensão E** (Fase 6)
4. **Handoff para agente prototipador** (Fase 7) — gerar `screen-map.md`, `design-tokens.json`, `prototypes/*.html`
5. **Validação e handoff final** (Fase 8) → `/techspec`