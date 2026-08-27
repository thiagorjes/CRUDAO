# Screen Map — Kanban de Tarefas

_Baseado em: [design-brief](../kanban-tarefas-design-brief.md) v1.0, seções 4 e 5._

## Layout global

Todas as telas internas (TL-02 a TL-10) usam **Sidebar** (projetos, dashboard, admin — colapsável, destaque do projeto ativo) + **Topbar** (sino de notificações RF-005 com `aria-live="polite"`, menu de usuário/logout). TL-01 (Login) não possui sidebar/topbar.

## Inventário de telas

| ID | Nome | Rota | Componentes principais |
|---|---|---|---|
| TL-01 | Login (SSO Keycloak) | `/login` | Card de login centralizado, botão "Entrar com Keycloak", estado de redirecionamento, mensagem de falha de auth |
| TL-02 | Lista de Projetos | `/projetos` | Sidebar, Topbar, grid/lista de cards de projeto, botão "Novo projeto", filtro/busca, estado vazio |
| TL-03 | Board — variação A (cards compactos) | `/projetos/:id/board` | Sidebar, Topbar, colunas do workflow, raias (swimlanes), card compacto (título + badge tipo + avatar), botão "Novo card" por coluna, indicador de impedimento, drag-and-drop, toast de erro de transição |
| TL-03b | Board — variação B (cards expandidos) | `/projetos/:id/board` | Igual TL-03, card expandido (título, descrição curta, badges tipo/prioridade, responsável, indicador de impedimento, lead-time da etapa) |
| TL-04 | Detalhe da Tarefa (modal/drawer) | modal sobre `/board` | Drawer lateral, campos travados pós-início (RF-003), toggle impedimento, lead-time por etapa (RF-006), histórico de auditoria (RF-017), botão salvar |
| TL-05 | Nova Tarefa (modal) | modal sobre `/board` | Modal centralizado, formulário (título, descrição, tipo, raia opcional, responsável opcional), validação inline, botão "Criar" |
| TL-06 | Confirmação de Exclusão de Card | modal sobre `/board` | Modal de confirmação, mensagem de impacto, botões Cancelar/Excluir, mensagem de bloqueio por permissão |
| TL-07 | Dashboard | `/projetos/:id/dashboard` | Sidebar, Topbar, cards de KPI (lead-time médio por etapa, tempo médio de impedimento), gráfico de barras por etapa, filtro de período, estado vazio (sem histórico) |
| TL-08 | Admin de Projeto | `/projetos/:id/admin` | Sidebar, Topbar, abas (Workflows/Colunas/Transições), tabela de colunas com reordenação, editor de transições, toggle finalizar/reabrir projeto, validação RN-003/RN-005 |
| TL-09 | Admin de Papéis/Permissões | `/projetos/:id/admin/papeis` | Sidebar, Topbar, tabela de papéis x permissões, toggles (ex. `devPodeExcluirTarefa`), botão salvar |
| TL-10 | Lista de Usuários (Admin) | `/projetos/:id/admin/usuarios` | Sidebar, Topbar, tabela de usuários associados, seletor de papel, botão "Associar usuário", estado vazio |

## Transições de navegação (seção 5 do brief)

**Happy path — UC-001 (criar tarefa e sinalizar impedimento):**
`TL-02 → TL-03 → (clica "Novo card") → TL-05 → card criado → TL-03 → (abre card) → TL-04 → marca impedimento → TL-03 (indicador atualizado)`

**Fluxo de erro — RF-002 (transição bloqueada):**
`TL-03 → drag para coluna sem transição configurada → drop rejeitado → toast/inline de erro → card retorna à posição original`

**Fluxo de erro — RF-019 (exclusão sem permissão):**
`TL-03 → aciona excluir card → TL-06 → toggle devPodeExcluirTarefa desabilitado → ação bloqueada → mensagem de erro no modal`

## Mapa de arquivos do protótipo

| Tela | Arquivo |
|---|---|
| TL-01 | `prototypes/tl-01-login.html` |
| TL-02 | `prototypes/tl-02-lista-projetos.html` |
| TL-03 | `prototypes/tl-03-board-compacto.html` |
| TL-03b | `prototypes/tl-03b-board-expandido.html` |
| TL-04 | `prototypes/tl-04-detalhe-tarefa.html` |
| TL-05 | `prototypes/tl-05-nova-tarefa.html` |
| TL-06 | `prototypes/tl-06-confirmar-exclusao.html` |
| TL-07 | `prototypes/tl-07-dashboard.html` |
| TL-08 | `prototypes/tl-08-admin-projeto.html` |
| TL-09 | `prototypes/tl-09-admin-papeis.html` |
| TL-10 | `prototypes/tl-10-lista-usuarios.html` |

Cada arquivo HTML é navegável (links relativos entre telas) e contém, como seções internas comentadas, as variações de estado obrigatórias (idle / loading / preenchido / erro / sucesso / vazio) conforme seção 6 do brief.
