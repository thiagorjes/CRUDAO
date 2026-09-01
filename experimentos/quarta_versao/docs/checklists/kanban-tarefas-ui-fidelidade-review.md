> **Remediação aplicada em 2026-09-01 (mesmo dia, 4 commits) — ver `memory/state.md`.**
> C1, C2, C3, I1-I3, I5 (incl. TL-09 papéis/permissões e escrita de TL-10 — RBAC implementado
> do zero no backend: pacote `com.crudao.kanban.papel` + migration V8), I6, I7 resolvidos. I4
> parcialmente resolvido (KPIs+gráfico adicionados; agregado "geral"/filtro de período seguem
> limitados pelo contrato do backend). Board/detalhe/dashboard/admin reescritos com o design
> system; zero Tailwind restante. Suíte `-P integration-tests`: 185 testes, 0 falhas. Este
> documento é mantido como registro do estado ANTES da remediação — não editado retroativamente
> para não perder o histórico do achado original.

# Code Review — Fidelidade das telas aos protótipos

_Data: 2026-09-01 | Escopo: `systems/CRUDAO/frontend` confrontado com `docs/design/kanban-tarefas/prototypes/`_
_Regra do pedido: telas SEM protótipo são aceitas como estão; telas COM protótipo devem ser idênticas._

## Gate de testes

`vitest` 8/8 e suíte backend `-P integration-tests` verdes nesta sessão. Não bloqueia — review é de fidelidade visual/estrutural, não de comportamento.

## Mapa protótipo → implementação

| ID | Protótipo | Implementação | Fidelidade |
|----|-----------|---------------|------------|
| TL-01 | `tl-01-login.html` | `app/login/page.tsx` | ✅ Alta |
| TL-02 | `tl-02-lista-projetos.html` | `app/(dashboard)/projetos/page.tsx` | 🟡 Média (estrutura ok; sidebar diverge) |
| TL-03 | `tl-03-board-compacto.html` | `board/page.tsx` + `BoardLayout.tsx` + `components/Card.tsx` | 🔴 Baixa |
| TL-03b | `tl-03b-board-expandido.html` | (não há variação; board único) | 🔴 Não implementado como no protótipo |
| TL-04 | `tl-04-detalhe-tarefa.html` | `tarefas/[tarefaId]/page.tsx` + painéis | 🔴 Baixa (página, não drawer) |
| TL-05 | `tl-05-nova-tarefa.html` | `board/CreateCardModal.tsx` | 🔴 Baixa |
| TL-06 | `tl-06-confirmar-exclusao.html` | `window.confirm()` em `board/page.tsx` e `Card.tsx` | 🔴 Não implementado |
| TL-07 | `tl-07-dashboard.html` | `dashboard/page.tsx` + `DashboardView.tsx` | 🔴 Baixa |
| TL-08 | `tl-08-admin-projeto.html` | `admin/` + `AdminLayoutClient.tsx` + `WorkflowsList/RaiasList` | 🔴 Baixa |
| TL-09 | `tl-09-admin-papeis.html` | `admin/papeis/page.tsx` + `PapeisList.tsx` | 🔴 Baixa |
| TL-10 | `tl-10-lista-usuarios.html` | (fundido em `admin/papeis` / `PapeisList`) | 🔴 Baixa |

## 🔴 Crítico

### [C1] Telas TASK-07.2+ estilizadas com classes utilitárias de um framework que não está instalado
Arquivos: `board/page.tsx`, `board/BoardLayout.tsx`, `board/CreateCardModal.tsx`, `components/Card.tsx`, `components/dashboard/DashboardView.tsx`, `dashboard/page.tsx`, `tarefas/[tarefaId]/page.tsx`, `components/EditarTarefaForm.tsx`, `components/AuditoriaPanel.tsx`, `components/LeadTimePanel.tsx`, `components/ObservadoresPanel.tsx`, `components/admin/AdminLayoutClient.tsx`, `components/admin/ProjetoAdminForm.tsx`, `components/admin/WorkflowsList.tsx`, `components/admin/RaiasList.tsx`, `components/admin/PapeisList.tsx`.

Problema: essas telas usam `className="flex bg-white rounded-lg text-gray-900 bg-blue-600 …"` (Tailwind) como estilização principal. Não há `tailwindcss` em `package.json`, nem `tailwind.config`/`postcss.config`, nem diretivas `@tailwind` em `app/globals.css`. Logo **nenhuma dessas classes resolve** — as telas renderizam praticamente sem estilo (fontes/base do browser), sem cores, espaçamentos, cards, colunas, tabelas ou grid do design.

Consequência direta na regra do pedido: TL-03, TL-03b, TL-04, TL-05, TL-06, TL-07, TL-08, TL-09 e TL-10 **não são idênticas** aos protótipos — não chegam nem perto, pois o `_shared.css`/`globals.css` (que define `.board`, `.column`, `.task-card`, `.swimlane`, `.modal`, `.tabs`, `.kpi-grid`, `.bar-chart`, `.btn`, `.card`, `.form-field`, `.empty-state`, `.toast`, `.skeleton`, `.badge*`) é ignorado por essas telas.

Como corrigir (uma das opções, projeto-wide):
- Reescrever essas telas usando as classes do design system já presentes em `app/globals.css` (mesmo caminho de `login/page.tsx` e `projetos/page.tsx`), replicando a marcação dos protótipos; **ou**
- Instalar e configurar Tailwind e então reconstruir os protótipos como utilitários equivalentes aos tokens (mais trabalhoso e ainda exige bater com o HTML do protótipo).

Guideline violado: `docs/design/kanban-tarefas/screen-map.md` (todas as telas internas usam o shell + componentes do `_shared.css`); `memory/state.md` afirma "Tokens visuais Design Brief 100% aplicados" para a TASK-07.1 — não vale para 07.2+.

### [C2] TL-06 (Confirmação de exclusão de card) não implementada
Arquivos: `board/page.tsx:handleExcluir` (`if (!confirm("Tem certeza que deseja excluir este card?")) return;`), `Card.tsx` (item de menu "Excluir" chama direto).

Problema: o protótipo `tl-06-confirmar-exclusao.html` define um `role="alertdialog"` com título "Excluir card", texto de impacto citando o título do card, botões `Cancelar` (`.btn-outline`) / `Excluir` (`.btn-danger`), além dos estados loading/erro-sem-permissão/sucesso. A implementação usa o `window.confirm()` nativo do browser — string genérica, sem o nome do card, sem estados, sem estilo. Zero fidelidade.

Como corrigir: criar um componente de modal de confirmação seguindo a marcação de `tl-06`, reutilizando `.modal` / `.modal-actions` / `.btn-danger` / `.toast-error` do `globals.css`.

Guideline violado: `tl-06-confirmar-exclusao.html` (existe → deve ser idêntico).

### [C3] TL-04 (Detalhe da tarefa) é página inteira, não o drawer do protótipo
Arquivo: `tarefas/[tarefaId]/page.tsx`.

Problema: o protótipo é um **drawer lateral** de 480px (`aside.drawer`, `role="dialog" aria-modal="true"`, `border-left`, sobre o board), com ordem de campos definida (Descrição travada → Responsável → Etapa atual → toggle Impedido → Lead-time por etapa → botão Salvar → seção "Histórico" com `.history-item`). A implementação é uma **rota de página cheia** com grid de 3 colunas (`lg:grid-cols-3`), botão "← Voltar ao board", blocos "Informações"/"Observadores" que não existem no protótipo nessa forma, e sem o container drawer. Estrutura e navegação divergem por completo (e sem estilo, ver C1).

Guideline violado: `tl-04-detalhe-tarefa.html` + `screen-map.md` linha TL-04 ("modal/drawer sobre `/board`").

## 🟡 Importante

### [I1] TL-03 — layout do board diverge da estrutura do protótipo
Arquivo: `board/BoardLayout.tsx`.

Problema: protótipo = por raia, um bloco `.swimlane` com `.swimlane__title` ("Raia: Backend") contendo um `.board` horizontal de `.column`; cada `.column` tem `.column__header` (nome + badge contador) e `.task-card`s, e um `.btn.btn-text` "+ Novo card" ao pé da coluna. Implementação = matriz "coluna fixa de rótulos de raia (w-32) × colunas de etapa com linhas de raia dentro", texto "sem cards"/"card(s)", sem `.swimlane__title` no formato "Raia: X", sem botão de novo card por coluna. É uma grade diferente do quadro por raias do protótipo.

Guideline violado: `tl-03-board-compacto.html`.

### [I2] TL-03 — card não reproduz o protótipo
Arquivo: `components/Card.tsx`.

Problema: protótipo do card = `.task-card` (ou `.task-card--expanded`) com `.badge.badge-tipo` (Bug/Feature), título 14px, e avatar `JS` **ou** `.badge.badge-warning` "Impedido"; impedido acrescenta `.task-card__impedido` (borda-esquerda 4px `--color-warning`). Implementação = `div` com utilitários Tailwind, sem badge de tipo, sem avatar do responsável, menu "⋮" (que não existe no protótipo — o protótipo move por drag-and-drop com indicador de coluna válida/ inválida `.column--drop-valid/invalid`), badge "Iniciado" que não existe no protótipo. DnD do protótipo (RF-002) não implementado — só menu de contexto.

Guideline violado: `tl-03-board-compacto.html`, `tl-03b-board-expandido.html`.

### [I3] TL-05 — modal de nova tarefa incompleto
Arquivo: `board/CreateCardModal.tsx`.

Problema: protótipo tem os campos Título* / Descrição / **Tipo** (Feature/Bug/Débito técnico) / **Raia (opcional)** / **Responsável (opcional)** e usa `.modal`, `.form-field`, `.form-error` com `role="alert"`, botão "Criar card" full-width. Implementação só tem Título + Descrição, backdrop `bg-black bg-opacity-50` (Tailwind, inerte), sem `.modal`/`.form-field`, botão "Criar". Campos Tipo/Raia/Responsável ausentes.

Guideline violado: `tl-05-nova-tarefa.html`.

### [I4] TL-07 — dashboard não reproduz KPIs nem gráfico
Arquivos: `dashboard/page.tsx`, `components/dashboard/DashboardView.tsx`.

Problema: protótipo = `.page-header` com `<select>` de período, `.kpi-grid` com 3 `.card.kpi-card` (Lead-time médio geral / Tempo médio de impedimento / Tarefas concluídas no período) e um `.card` com `.bar-chart` (barras por etapa). Implementação = uma única `<table>` (Etapa | Lead-time médio | Impedimento médio), sem KPIs, sem gráfico de barras, sem filtro de período. Parte disso é limitação do contrato do backend (TASK-07.6 só devolve por etapa + total), mas o gráfico de barras por etapa e o filtro de período são elementos do protótipo ausentes.

Guideline violado: `tl-07-dashboard.html`.

### [I5] TL-08/09/10 — admin não reproduz a estrutura dos protótipos
Arquivos: `components/admin/AdminLayoutClient.tsx`, `WorkflowsList.tsx`, `RaiasList.tsx`, `PapeisList.tsx`, rotas `admin/*`.

Problema:
- TL-08 prevê **uma tela** com abas internas `Colunas / Transições / Raias` (`.tabs role="tablist"`), ações no header `Papéis/Permissões`, `Usuários`, `Finalizar projeto` (`.btn-danger`), e tabela de colunas com "Transições de saída" + "+ Nova coluna". A implementação quebra em 4 rotas (`/admin/projeto`, `/admin/workflows`, `/admin/raias`, `/admin/papeis`), a barra de abas é reconstruída com utilitários Tailwind (`.tabs` do globals não é usada), e o botão "Finalizar projeto" / ações de header do protótipo não aparecem nesse formato.
- TL-10 (Lista de Usuários do projeto) — tela própria no protótipo (`/projetos/:id/admin/usuarios`, tabela Usuário/Papel/Remover, "+ Associar usuário", estado vazio). Não há rota `admin/usuarios`; a associação foi fundida em `PapeisList`. Rota do `screen-map` não existe.

Guideline violado: `tl-08-admin-projeto.html`, `tl-09-admin-papeis.html`, `tl-10-lista-usuarios.html`, `screen-map.md`.

### [I6] Sidebar diverge do protótipo em todas as telas internas
Arquivo: `components/DashboardShell.tsx`.

Problema: protótipo (`_shared.css` + todas as TL-02..TL-10) = `.sidebar__brand` "Kanban" (sem emoji), `nav` com 3 links fixos **Projetos / Dashboard / Admin** (`aria-current` na página atual) e, no rodapé, `.sidebar__project-active` "Projeto ativo: <nome>". Implementação = brand "📋 Kanban", uma seção "Meus Projetos" listando cada projeto como "Projeto <8 chars do id>", e uma seção contextual "Ações" (📊 Board / 📈 Dashboard / ⚙️ Admin com emojis) só quando dentro de um projeto; o `.sidebar__project-active` é usado como container da nav "Ações", não como chip com o nome do projeto. IA e rótulos diferentes; usa a classe `sidebar-section-title` que **não existe** em `globals.css` nem no `_shared.css` (texto sem estilo).

Guideline violado: `_shared.css` (`.sidebar`, `.sidebar__project-active`), telas TL-02..TL-10.

### [I7] `globals.css` fora de paridade com `_shared.css`
Arquivo: `app/globals.css`.

Problema: o cabeçalho do arquivo diz "Mantido em paridade manual" com `_shared.css`, mas divergiu:
- `table th` — `_shared.css`: `text-transform: uppercase; font-size: 12px; padding: var(--space-sm)`. `globals.css`: sem `uppercase`, sem `font-size`, `padding: 8px 4px`.
- `html, body` — `_shared.css` define `font-family: 'Inter', system-ui…`; `globals.css` não define `font-family` (depende do `<link>` do Google Fonts no `layout.tsx`, sem fallback aplicado ao body).
- `_shared.css` tem `.badge-tipo`, `.badge-error` que o `globals.css` não replica (tem `.badge-neutro` no lugar).

Guideline violado: comentário de paridade do próprio `globals.css`.

## 🔵 Sugestão

- [S1] TL-02: o botão do protótipo é "**+ Novo projeto**"; a implementação usa "Novo projeto" (sem o `+`). Estado vazio do protótipo tem CTA "Criar primeiro projeto"; a implementação só mostra texto.
- [S2] TL-01: praticamente idêntico. Único ponto — o protótipo não tem `viewport`/fonts no `<head>` da tela (estão no shell); ok. Sem ação.
- [S3] Padronizar o backdrop de modais em `.modal-overlay` (já existe em `globals.css`, com `position: fixed; inset: 0`) em vez de `fixed inset-0 bg-black bg-opacity-50` (Tailwind).
- [S4] `next build` e ESLint não detectam classes mortas (ESLint v9 sem config no repo — apontado em reviews anteriores). Vale um teste/gate simples que falhe se `className` contiver utilitários `bg-*`/`text-gray-*` fora da allowlist, enquanto Tailwind não for adotado oficialmente.

## ✅ Pontos Positivos

- **TL-01 (Login)** — fiel ao protótipo: `.card.login-card`, `.btn.btn-primary.full`, h1/parágrafo e a cópia do toast de erro batem palavra a palavra.
- **TL-02 (Lista de Projetos)** — estrutura central fiel: `.project-grid` + `.card.project-card` + `.badge-success` "Ativo" + `.empty-state`; o novo botão "Novo projeto" usa `.btn.btn-primary` do design system (não Tailwind).
- Tokens de cor/espaçamento/raio em `:root` do `globals.css` batem 1:1 com `design-tokens.json` / `_shared.css`.
- `DashboardShell` usa o grid `.app-shell` + `.sidebar` + `.topbar` do design (apesar das divergências de conteúdo em I6).
- Estados de erro com `role="alert"` e cópia próxima à dos protótipos em várias telas.

## Conformidade com TechSpec / Design

Telas com protótipo que **estão idênticas**: TL-01. Parcialmente: TL-02 (shell/sidebar divergem). As demais (TL-03, TL-03b, TL-04, TL-05, TL-06, TL-07, TL-08, TL-09, TL-10) **não conferem** com os protótipos — causa raiz única e transversal: estilização por framework não instalado (C1) + reimplementações estruturais (C2, C3, I1–I6).

Telas/elementos **sem protótipo** (aceitos como estão, conforme a regra do pedido): painel dropdown de notificações (`NotificacoesSino` — o protótipo só mostra o sino), menu de usuário/logout no topbar (o `screen-map` menciona "menu de usuário/logout" mas não há protótipo do menu), fluxo de ticket WS, formulário inline de criação de projeto (o protótipo define só o botão).

## Resultado

**REPROVADO** — 3 críticos, 7 importantes, 4 sugestões.

Para re-review, o mínimo:
1. C1 — decidir e aplicar a estratégia de CSS (reescrever com o design system OU adotar Tailwind) em todas as telas TASK-07.2+.
2. C2 — implementar o modal de confirmação de exclusão (TL-06).
3. C3 — TL-04 como drawer sobre o board, com a ordem de campos e a seção Histórico do protótipo.
4. I1–I6 — reconstruir board, card, modal de nova tarefa, dashboard, admin e sidebar conforme a marcação dos protótipos.
