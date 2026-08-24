# Design Brief — Kanban Configurável
_Versão: 1.0 | Data: 2026-08-22 | Designer: Thiago Goncalves Cavalcante_
_PRD: docs/prd/kanban-configuravel-prd.md_

---

## 1. Contexto e Objetivo

**Feature:** kanban-configuravel
**Objetivo de UX:** Dar visibilidade clara e imediata do andamento das tarefas e impedimentos, com interação direta (drag-and-drop) e feedback visual das regras de workflow, reduzindo a dependência de comunicação dispersa entre equipe e gestores.
**Público-alvo:** Equipe de desenvolvimento (uso diário, atualização recorrente) e gestores de outros times (consulta, dashboard).

---

## 2. Tokens de Cor

| Token | Valor | Uso |
|-------|-------|-----|
| `--color-primary` | `#0d6efd` | Ações principais, CTAs, destaque de coluna válida no drag |
| `--color-secondary` | `#198754` | Ações secundárias, estados de sucesso |
| `--color-background` | `#f8f9fa` | Fundo de tela |
| `--color-surface` | `#ffffff` | Cards, modais, painéis |
| `--color-error` | `#dc3545` | Estados de erro, indicador de impedimento ("semáforo" vermelho) |
| `--color-success` | `#198754` | Toast de sucesso, transição concluída |
| `--color-text-primary` | `#212529` | Texto principal |
| `--color-text-secondary` | `#6c757d` | Texto auxiliar, metadados |
| `--color-warning` | `#ffc107` | Alertas intermediários (ex.: tarefa próxima do prazo, se aplicável no futuro) |
| `--color-border` | `#dee2e6` | Bordas de cards e colunas |

Paleta com 10 tokens, respeitando o limite definido (DDR-001).

---

## 3. Tipografia

| Token | Fonte | Peso | Tamanho | Uso |
|-------|-------|------|---------|-----|
| `--font-heading-1` | Roboto | Bold (700) | 28px | Títulos de página (ex.: nome do projeto) |
| `--font-heading-2` | Roboto | Medium (500) | 20px | Subtítulos de seção (ex.: nome da coluna) |
| `--font-body` | Roboto | Regular (400) | 14px | Texto corrido, conteúdo de card |
| `--font-caption` | Roboto | Regular (400) | 12px | Legendas, metadados (ex.: lead-time, responsável) |
| `--font-code` | Roboto Mono | Regular (400) | 13px | IDs técnicos, se exibidos (ex.: TASK-ID) |

Fonte única — Roboto para heading e body (DDR-001).

---

## 4. Espaçamento e Grid

- **Grid:** fluido baseado em flex/grid CSS, colunas do board com largura mínima fixa e scroll horizontal quando necessário
- **Breakpoints:** Desktop ≥1024px (único breakpoint suportado — sem mobile/tablet, conforme RNF-005 e DDR-001)
- **Escala de espaçamento:** base 8px

| Token | Valor | Uso |
|-------|-------|-----|
| `--spacing-xs` | 4px | Elementos internos compactos (ex.: ícone + label) |
| `--spacing-sm` | 8px | Padding de componentes (ex.: card) |
| `--spacing-md` | 16px | Espaçamento entre componentes (ex.: entre cards) |
| `--spacing-lg` | 24px | Seções da página (ex.: entre colunas) |
| `--spacing-xl` | 32px | Margens de layout (ex.: header do board) |

---

## 5. Componentes

### Card de Tarefa

**Quando usar:** representar uma tarefa no board (por coluna/raia).

**Variantes:**
- Normal: exibe título, tipo, responsável, avatar/inicial
- Impedida: exibe ícone de "semáforo" vermelho (`--color-error`) sobreposto ao card

**Estados:** default | hover | dragging | drop-target-valid | disabled

**Exemplo:**
```
┌─────────────────────────┐
│ 🔴 [Tipo] Título da task │
│ 👤 Responsável           │
│ ⏱ 2d 4h nesta etapa      │  ⋮ (menu: avançar/retroceder/desfinalizar)
└─────────────────────────┘
```

### Coluna do Board

**Quando usar:** representar uma etapa do workflow.

**Variantes:**
- Normal: fundo `--color-background`
- Destacada (drop-target válido durante drag): borda `--color-primary`, leve fundo tintado
- Etapa final: indicador visual sutil (ex.: ícone de flag) para diferenciar de etapas intermediárias

**Estados:** default | highlighted (durante drag) | disabled (sem transição válida)

### Raia (Swimlane)

**Quando usar:** organizar cards horizontalmente dentro do board quando há múltiplos desenvolvedores no mesmo projeto.

**Variantes:**
- Raia default global
- Raia específica do projeto

**Estados:** default | collapsed (recolhida para economizar espaço vertical)

### Modal de Confirmação

**Quando usar:** erros e situações que exigem atenção do usuário (ex.: transição negada, exclusão bloqueada por dependência).

**Variantes:**
- Erro: ícone `--color-error`, ação de fechar
- Confirmação de ação destrutiva: botão de ação em `--color-error`, botão cancelar neutro

**Estados:** default | loading (ação em progresso)

### Toast/Snackbar

**Quando usar:** feedback de sucesso ou informações de baixa relevância (ex.: transição concluída, tarefa criada).

**Variantes:**
- Sucesso: `--color-success`
- Informativo: `--color-text-secondary`

**Estados:** entrando | visível | saindo (auto-dismiss)

### Skeleton Screen

**Quando usar:** carregamento de operações assíncronas longas (ex.: dashboard aguardando job em background, ADR-005).

**Estados:** loading (skeleton) | loaded (conteúdo real)

### Spinner

**Quando usar:** carregamento de operações síncronas rápidas, como fallback quando skeleton não se aplica.

**Estados:** loading

### Gráfico de Barras + Tabela (Dashboard)

**Quando usar:** exibir lead-time médio por etapa e tempo médio em impedimento, agregados por projeto e período selecionado.

**Variantes:**
- Gráfico de barras: uma barra por etapa, cor `--color-primary`
- Tabela: mesma informação em formato tabular, para leitura precisa dos valores

**Estados:** loading (skeleton) | loaded | vazio (sem dados no período)

### Seletor de Período

**Quando usar:** fixo no topo da tela de Dashboard, define o intervalo de datas usado no cálculo agregado.

**Variantes:** intervalo customizado (data início/fim)

### Painel de Administração

**Quando usar:** área única e separada do board, para gestão de Projetos, Workflows, Colunas, Raias e Papéis/Permissões, com seletor de projeto no topo para alternar contexto.

**Variantes:**
- Acesso completo (admin global)
- Acesso restrito ao projeto (quando aberto via "Configurações do projeto" a partir do board — RF-013)

### Menu do Card (dropdown)

**Quando usar:** alternativa ao drag-and-drop para avançar/retroceder no workflow; exibe opção "Desfinalizar" quando a tarefa está na etapa final.

**Variantes:** ações habilitadas conforme transições permitidas pelo workflow

---

## 6. Padrões de Interação

| Padrão | Descrição | Animação |
|--------|-----------|----------|
| Drag-and-drop de card | Ao iniciar o arraste, colunas com transição válida (RF-002) recebem destaque visual imediato; drop fora de coluna destacada é revertido | Transição de cor suave (~150ms) ao iniciar drag |
| Movimentação via menu | Alternativa ao drag — mesmo endpoint/validação do backend | Sem animação especial, feedback via toast |
| Atualização em tempo real | Mudanças de outros usuários aparecem no board automaticamente (RNF-001, <2s) | Fade-in leve do card/coluna atualizada |
| Job assíncrono do dashboard | Disparo do cálculo mostra skeleton imediatamente; resultado chega via WebSocket e substitui o skeleton | Fade entre skeleton e conteúdo real |

**Feedback visual:**
- Sucesso: toast/snackbar (`--color-success`), auto-dismiss
- Erro/atenção: modal de confirmação, exige ação do usuário
- Loading: skeleton screen para operações longas (ex.: dashboard); spinner simples como fallback para operações síncronas rápidas

---

## 7. Acessibilidade

- **Contraste mínimo:** não obrigatório nesta versão (sistema interno / validação de conceito) — decisão registrada em DDR-003
- **Foco visível:** não obrigatório nesta versão
- **Leitores de tela:** não obrigatório nesta versão
- **Tamanho mínimo de toque:** não aplicável (desktop-only, RNF-005)

> Revisar esta seção se o escopo do sistema se expandir para público externo ou exigir compliance de acessibilidade no futuro.

---

## 8. Decision Records de Design (DDR)

| DDR | Decisão |
|-----|---------|
| DDR-001 | Tokens base de design: cores, tipografia e espaçamento |
| DDR-002 | Interação do board: drag-and-drop com destaque de colunas válidas + menu alternativo |
| DDR-003 | Padrões de feedback, loading assíncrono e nível de acessibilidade |

---

## 9. Referências

- Design System base: nenhum — paleta e tokens definidos do zero nesta feature (DDR-001)
- Protótipo clicável: `docs/design/prototypes/kanban-configuravel/` (fontes `.dc.html` + `kanban-prototipos.html` seedado) — publicado como Artifact em https://claude.ai/code/artifact/a7612319-88d0-434d-9729-64d3d1604c6c, **aprovado pelo usuário em 2026-08-22**
- Inspirações: ferramentas kanban de mercado (padrão de coluna/card/drag-and-drop consolidado)
