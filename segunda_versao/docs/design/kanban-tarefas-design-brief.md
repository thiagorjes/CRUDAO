# Design Brief — Kanban de Tarefas
_Versão: 1.0 | Status: Draft | Data: 2026-08-25 | Autor: Thiago Goncalves Cavalcante_

> Referência: [docs/prd/kanban-tarefas-prd.md](../prd/kanban-tarefas-prd.md) v1.0

---

## 1. Contexto e Objetivo

Sistema de kanban configurável para equipes de desenvolvimento e gestores, permitindo acompanhamento de tarefas, sinalização de impedimentos e visibilidade de lead-time sem depender de comunicação dispersa (RF-001 a RF-019). Este brief define a identidade visual e o escopo de telas do protótipo navegável que informará as decisões de arquitetura frontend do `/techspec`.

---

## 2. Identidade Visual

**Tom:** Sério/corporativo — cores sóbrias, tipografia neutra, foco em densidade de informação.

**Tema:** Light only.

**Referência visual:** Trello — cards espaçosos, cores por status/etiqueta, visual leve dentro do tom corporativo.

### Paleta

| Token | Valor | Uso |
|---|---|---|
| primary | `#0d6efd` | ação primária |
| secondary | `#198754` | ação secundária |
| background | `#f8f9fa` | fundo da página |
| surface | `#ffffff` | cards/painéis |
| error | `#dc3545` | erro |
| success | `#198754` | sucesso |
| warning | `#ffc107` | alerta / impedimento |
| textPrimary | `#212529` | texto principal |
| textSecondary | `#6c757d` | texto secundário |
| border | `#dee2e6` | bordas |
| tipoBadgeBackground | `#e7f1ff` | badge de tipo de tarefa |

### Tipografia e grid

- Fonte: **Inter** (heading e body), fallback system-ui.
- Base de espaçamento: **8px** (múltiplos: 4/8/16/24/32/48).
- Radius: 8px (cards/botões), 4px (badges).
- Breakpoints: desktop-first (RNF-005 — sem suporte mobile obrigatório). Prioritário: 1280px+; secundário: 1024px.

---

## 3. Navegação e Layout

- **Padrão:** Sidebar (navegação global: projetos, dashboard, admin) + Topbar (usuário, notificações, logout).
- Sidebar colapsável, com destaque do projeto ativo.
- Topbar com sino de notificações (RF-005) e menu de usuário.

---

## 4. Inventário de Telas

| ID | Nome | RF(s) | Persona | Rota sugerida |
|---|---|---|---|---|
| TL-01 | Login (SSO Keycloak) | RF-014 | Todos | `/login` |
| TL-02 | Lista de Projetos | RF-008 | Todos | `/projetos` |
| TL-03 | Board (colunas + raias) — variação A (cards compactos) | RF-001, RF-002, RF-011 | Dev, Gestor | `/projetos/:id/board` |
| TL-03b | Board — variação B (cards expandidos) | RF-001, RF-002, RF-011 | Dev, Gestor | `/projetos/:id/board` |
| TL-04 | Detalhe da Tarefa (modal/drawer) | RF-003, RF-004, RF-006, RF-017 | Dev, Gestor | modal sobre `/board` |
| TL-05 | Nova Tarefa (modal) | RF-018 | Dev | modal sobre `/board` |
| TL-06 | Confirmação de Exclusão de Card | RF-019 | Dev | modal sobre `/board` |
| TL-07 | Dashboard | RF-006, RF-007 | Gestor, Dev | `/projetos/:id/dashboard` |
| TL-08 | Admin de Projeto (workflow/colunas/transições) | RF-002, RF-009, RF-010 | Admin | `/projetos/:id/admin` |
| TL-09 | Admin de Papéis/Permissões | RF-013, RF-016 | Admin | `/projetos/:id/admin/papeis` |
| TL-10 | Lista de Usuários (dentro de Admin de Projeto) | RF-015 | Admin | `/projetos/:id/admin/usuarios` |

---

## 5. Fluxos de Navegação

**Happy path — UC-001 (criar tarefa e sinalizar impedimento):**
TL-02 → TL-03 → (clica "Novo card") → TL-05 → card criado → TL-03 → (abre card) → TL-04 → marca impedimento → TL-03 (indicador visual atualizado)

**Fluxo de erro crítico — RF-002 (transição bloqueada):**
TL-03 → usuário arrasta card para coluna sem transição configurada → sistema rejeita o drop, exibe toast/inline de erro, card retorna à posição original.

**Nota de interação — permissão de transição é por coluna, não por raia:** raias (swimlanes) são apenas agrupamento visual (RF-011), sem regra de transição associada. Durante o drag, o board destaca (outline verde) apenas as **colunas** com transição configurada a partir da etapa atual do card (RF-002/RN-003) e esmaece as colunas sem transição permitida — a raia de destino não é restringida.

**Fluxo de erro crítico — RF-019 (exclusão sem permissão):**
TL-03 → usuário aciona excluir card → TL-06 → toggle `devPodeExcluirTarefa` desabilitado → ação bloqueada, mensagem de erro exibida no próprio modal.

---

## 6. Estados por Tela

| Tela | idle | loading | preenchido | erro | sucesso | vazio |
|---|---|---|---|---|---|---|
| TL-01 Login | ✅ | ✅ (redirect SSO) | — | ✅ (falha auth) | ✅ (redirect ok) | — |
| TL-02 Lista de Projetos | ✅ | ✅ | ✅ | ✅ | — | ✅ (sem projetos) |
| TL-03/03b Board | ✅ | ✅ | ✅ | ✅ (transição bloqueada) | — | ✅ (coluna/raia sem cards) |
| TL-04 Detalhe da Tarefa | ✅ | ✅ | ✅ | ✅ (edição bloqueada pós-início) | ✅ (salvo) | — |
| TL-05 Nova Tarefa | ✅ | ✅ (submit) | ✅ | ✅ (validação) | ✅ (criado) | — |
| TL-06 Confirmação Exclusão | ✅ | ✅ (submit) | — | ✅ (sem permissão) | ✅ (excluído) | — |
| TL-07 Dashboard | ✅ | ✅ | ✅ | ✅ | — | ✅ (sem histórico) |
| TL-08 Admin de Projeto | ✅ | ✅ | ✅ | ✅ (validação RN-003/RN-005) | ✅ (salvo) | ✅ (sem workflows) |
| TL-09 Admin de Papéis | ✅ | ✅ | ✅ | ✅ | ✅ (salvo) | — |
| TL-10 Lista de Usuários | ✅ | ✅ | ✅ | ✅ | — | ✅ (sem usuários associados) |

---

## 7. Acessibilidade e Internacionalização

- **Nível:** WCAG AA — contraste mínimo AA em todos os tokens de cor, navegação completa por teclado, foco visível em todos os elementos interativos, labels ARIA em modais e formulários.
- **Notificações em tempo real (RF-005):** usar `aria-live="polite"` para não interromper leitura em andamento.
- **i18n:** apenas pt_BR — sem suporte multi-idioma (RNF-005, desktop-only).
- **Plataforma-alvo:** desktop, breakpoint prioritário 1280px+.

---

## 8. Decisões em Aberto

| Questão | Opções | Impacto |
|---|---|---|
| Densidade do card no board | (A) Compacto — mais cards visíveis, menos detalhe inline / (B) Expandido — badges, responsável e indicador de impedimento visíveis sem abrir o card | Afeta legibilidade em boards com muitas tarefas; decisão a ser validada com o time após revisão do protótipo (TL-03 vs TL-03b) |

---

## 9. Escopo do Protótipo

- Telas: TL-01 a TL-10 (11 arquivos HTML, incluindo as 2 variações de board).
- Estados obrigatórios: idle, loading, preenchido, erro, sucesso, vazio — conforme tabela da seção 6.
- Variações: 2 (TL-03 e TL-03b) para decisão de densidade do card.

---

## 10. Decision Records de Design

> Decisões: —

Nenhuma decisão de design system foi considerada suficientemente estrutural para gerar DDR nesta fase — paleta e tipografia foram definidas diretamente pelo usuário sem trade-offs relevantes a registrar. A escolha entre variações de card (seção 8) poderá gerar DDR após validação do protótipo.

---

## Histórico de Revisões

| Versão | Data | Autor | Alteração |
|---|---|---|---|
| 1.0 | 2026-08-25 | Thiago Goncalves Cavalcante | Versão inicial |
