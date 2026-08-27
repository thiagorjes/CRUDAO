# Screen Map — kanban-configuravel

## Overview

| Screen ID | Name | Route | Description |
|-----------|------|-------|-------------|
| S01 | Board do Projeto | `/` | Principal board com colunas configuráveis, cards, drag/drop, impedimentos |
| S02 | Dashboard de Gestão | `/dashboard` | Lead-time médio por etapa, impedimentos, seletor de período |
| S03 | Painel de Administração | `/admin` | Abas: Projetos, Workflows, Colunas, Raias, Membros, Toggles, Papéis |
| S04 | Detalhe da Tarefa | `/tarefas/:id` | Tempo por etapa, impedimentos, observadores, atribuição, edição, histórico |
| S05 | Modal Criar Card | overlay em `/` | Título, descrição, tipo; etapa/raia automáticas |
| S06 | Modal Confirmar Exclusão | overlay em `/` | Confirmação de exclusão de card |
| S07 | Modal Finalizar/Desfinalizar | overlay em `/` | Escolha de etapa destino |
| S08 | Config Workflow/Colunas/Transições | abas em `/admin` | CRUD de workflow, colunas, transições |
| S09 | Gestão Membros/Toggles/Papéis | abas em `/admin` | CRUD de membros, toggles, papéis do projeto |

---

## Components per Screen

### S01 - Board do Projeto
| Component | Variations | States |
|-----------|------------|--------|
| Topbar | — | idle, loading (project switch) |
| ProjectSelector | — | idle, loading, error |
| Column (etapa) | — | idle, loading (skeleton), preenchido, vazio, erro |
| Card | **Compacto / Confortável** | idle, loading (skeleton), preenchido, erro (movimento inválido), impedido |
| Swimlane (raia) | **Sempre expandidas / Recolhíveis** | idle, collapsed, expanded, vazio |
| CardDeleteIcon | **Sempre visível / No hover** | idle, hover, loading (excluindo) |
| ToastContainer | — | success (mover, criar, excluir, impedimento), error |
| EmptyState | — | sem workflow, sem cards |
| DragOverlay | — | dragging, valid-drop, invalid-drop |

**Total variations for S01:** 12 (2 card density × 2 swimlane × 2 delete icon × 1 base + 4 state combinations)

### S02 - Dashboard de Gestão
| Component | Variations | States |
|-----------|------------|--------|
| Topbar | — | idle |
| ProjectSelector | — | idle |
| PeriodSelector | — | idle, error (invalid range) |
| LeadTimeChart | **Empilhados / Lado a lado** | idle, loading (skeleton), preenchido, erro, vazio (sem dados) |
| LeadTimeTable | **Empilhados / Lado a lado** | idle, loading (skeleton), preenchido, erro, vazio |
| EmptyState | — | sem dados no período |
| ErrorState | — | job falhou |

**Total variations for S02:** 2 (layout chart/table)

### S03 - Painel de Administração
| Component | Variations | States |
|-----------|------------|--------|
| Topbar | — | idle |
| ProjectSelector | — | idle |
| AdminNavigation | **Abas horizontais / Lista lateral** | idle, loading (tab switch) |
| TabPanel (Projetos/Workflows/Colunas/Raias/Membros/Toggles/Papéis) | — | idle, loading, preenchido, erro, vazio (sem projetos - inclui criar primeiro) |
| EmptyState | — | sem projetos (com botão criar) |

**Total variations for S03:** 2 (navigation style)

### S04 - Detalhe da Tarefa
| Component | Variations | States |
|-----------|------------|--------|
| Topbar | — | idle |
| TaskHeader | — | idle, loading |
| StageTimeline | — | idle, loading, preenchido, vazio parcial (sem impedimentos/observadores/histórico) |
| ImpedimentList | — | idle, loading, preenchido, vazio |
| WatchersList | — | idle, loading, preenchido, vazio |
| AssigneeSelector | — | idle, loading, preenchido, erro |
| EditForm | — | idle, loading (salvando), erro (validação), sucesso (toast) |
| HistoryList | — | idle, loading, preenchido, vazio |
| ToastContainer | — | success (editar, atribuir, impedimento), error |

**Total variations for S04:** 1 (fixed composition)

### S05 - Modal Criar Card
| Component | Variations | States |
|-----------|------------|--------|
| ModalBackdrop | — | idle |
| ModalContainer | **Essencial (título only) / Completo visível** | idle, loading (salvando), erro (validação título), sucesso |
| FormField (título) | — | idle, error (required), focus |
| FormField (descrição) | **Recolhido / Visível** | idle |
| FormField (tipo) | **Recolhido / Visível** | idle |
| FormActions | — | idle, loading, disabled |

**Total variations for S05:** 2 (form density)

### S06 - Modal Confirmar Exclusão
| Component | Variations | States |
|-----------|------------|--------|
| ModalBackdrop | — | idle |
| ModalContainer | — | idle, loading (excluindo), erro, sucesso |
| ConfirmMessage | — | idle |
| FormActions | — | idle, loading, disabled |

**Total variations for S06:** 1

### S07 - Modal Finalizar/Desfinalizar
| Component | Variations | States |
|-----------|------------|--------|
| ModalBackdrop | — | idle |
| ModalContainer | — | idle, loading, erro (sem permissão, projeto finalizado), sucesso |
| StageSelector | — | idle, erro (etapa inexistente), disabled |
| FormActions | — | idle, loading, disabled |

**Total variations for S07:** 1

### S08 - Config Workflow/Colunas/Transições
| Component | Variations | States |
|-----------|------------|--------|
| AdminNavigation | — | idle |
| WorkflowList | — | idle, loading, preenchido, erro, vazio |
| StageList | — | idle, loading, preenchido, erro (excluir etapa com tarefas), vazio |
| TransitionMatrix | — | idle, loading, preenchido, erro |
| FormWorkflow | — | idle, loading (salvando), erro (incompleto), sucesso |
| ToastContainer | — | success, error |

**Total variations for S08:** 1

### S09 - Gestão Membros/Toggles/Papéis
| Component | Variations | States |
|-----------|------------|--------|
| AdminNavigation | — | idle |
| MemberList | — | idle, loading, preenchido, erro (remover último papel admin), vazio |
| ToggleList | — | idle, loading, preenchido, erro, sucesso |
| RoleMatrix | — | idle, loading, preenchido, erro, sucesso |
| FormMember | — | idle, loading, erro (usuário/papel inexistente), sucesso |
| ToastContainer | — | success, error |

**Total variations for S09:** 1

---

## State Matrix (from Design Brief Section 6)

| Screen | idle | loading | preenchido | erro | sucesso | vazio |
|--------|------|---------|------------|------|---------|-------|
| S01 Board | ✓ | ✓ (skeleton) | ✓ | ✓ | ✓ (toast) | ✓ (sem workflow) |
| S02 Dashboard | ✓ | ✓ (skeleton, job bg) | ✓ | ✓ | — | ✓ (sem dados período) |
| S03 Admin | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ (sem projetos) |
| S04 Detalhe Tarefa | ✓ | ✓ | ✓ | ✓ | ✓ (toast) | ✓ parcial |
| S05 Modal Criar | ✓ | ✓ (salvando) | — | ✓ (validação) | ✓ | — |
| S06 Modal Excluir | ✓ | ✓ (excluindo) | — | ✓ | ✓ | — |
| S07 Modal Finalizar | ✓ | ✓ | — | ✓ (sem permissão, finalizado) | ✓ | — |
| S08 Config Workflow | ✓ | ✓ | ✓ | ✓ (excluir etapa com tarefas) | ✓ | ✓ |
| S09 Gestão Membros | ✓ | ✓ | ✓ | ✓ (remover último admin) | ✓ | ✓ |

---

## Open Decisions & Variations (12 total)

| Decision | Screen | Options | Variations |
|----------|--------|---------|------------|
| Card density | S01 | Compacto / Confortável | 2 |
| Swimlane presentation | S01 | Sempre expandidas / Recolhíveis | 2 |
| Delete icon visibility | S01 | Sempre visível / No hover | 2 |
| Admin navigation | S03 | Abas horizontais / Lista lateral | 2 |
| Dashboard layout | S02 | Empilhados / Lado a lado | 2 |
| Create card modal | S05 | Essencial / Completo | 2 |
| **Subtotal open decisions** | | | **12** |
| Fixed screens | S04, S06, S07, S08, S09 | 1 each | 5 |
| **Total variations** | | | **17** |

> Note: Design Brief states 12 variations (2×6 open decisions) + 1×3 fixed screens = 15. Above shows 5 fixed screens (S04, S06, S07, S08, S09) per inventory. Prototypes will cover all 17 for completeness.

---

## Prototype Files

| File | Screen | Variation |
|------|--------|-----------|
| `prototypes/S01-board-compacto-expandido-visivel.html` | S01 | Compacto, expandidas, visível |
| `prototypes/S01-board-compacto-expandido-hover.html` | S01 | Compacto, expandidas, hover |
| `prototypes/S01-board-compacto-recolhivel-visivel.html` | S01 | Compacto, recolhíveis, visível |
| `prototypes/S01-board-compacto-recolhivel-hover.html` | S01 | Compacto, recolhíveis, hover |
| `prototypes/S01-board-confortavel-expandido-visivel.html` | S01 | Confortável, expandidas, visível |
| `prototypes/S01-board-confortavel-expandido-hover.html` | S01 | Confortável, expandidas, hover |
| `prototypes/S01-board-confortavel-recolhivel-visivel.html` | S01 | Confortável, recolhíveis, visível |
| `prototypes/S01-board-confortavel-recolhivel-hover.html` | S01 | Confortável, recolhíveis, hover |
| `prototypes/S01-board-states.html` | S01 | All states (idle, loading, error, empty, success) |
| `prototypes/S02-dashboard-empilhado.html` | S02 | Chart + table empilhados |
| `prototypes/S02-dashboard-lado-a-lado.html` | S02 | Chart + table lado a lado |
| `prototypes/S03-admin-abas.html` | S03 | Abas horizontais |
| `prototypes/S03-admin-lateral.html` | S03 | Lista lateral |
| `prototypes/S04-detalhe-tarefa.html` | S04 | Fixed |
| `prototypes/S05-modal-criar-essencial.html` | S05 | Essencial (título only) |
| `prototypes/S05-modal-criar-completo.html` | S05 | Completo visível |
| `prototypes/S06-modal-excluir.html` | S06 | Fixed |
| `prototypes/S07-modal-finalizar.html` | S07 | Fixed |
| `prototypes/S08-config-workflow.html` | S08 | Fixed |
| `prototypes/S09-gestao-membros.html` | S09 | Fixed |

**Total: 20 prototype files**