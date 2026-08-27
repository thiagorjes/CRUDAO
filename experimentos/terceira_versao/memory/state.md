# Estado Operacional — CRUDAO
_Atualizado em: 2026-08-27_

> Estado atual do workspace e das features em andamento.
> Para princípios estáveis e ADRs, veja [memory/constitution.md](constitution.md).

---

## Toolset

**Versão:** 2026-08-26 — atualize com `scripts/update.py`
**Pipeline SSPDD:** /guidelines → /discovery → /prd → [/clarify] → [/checklist] → [/designer] → /techspec → /tasks → [/analyze] → /implement ou /tdd → /code-review → /tests → [/spdd-sync]

---

## Sistemas

| Sistema | Caminho | Cenário | Guidelines | Observações |
|---|---|---|---|---|
| CRUDAO | `systems/CRUDAO/` | Novo (greenfield) | pendente | — |

---

## Features Ativas

| Feature | Sistemas afetados | PRD | TechSpec | Tasks | Status |
|---|---|---|---|---|---|
| kanban-configuravel | CRUDAO | v1.0 | — | — | PRD concluído |

---

## Artifact Registry

| Artefato | v | Status |
|---|---|---|
| docs/discovery/kanban-configuravel-discovery.md | 1.0 | ok |
| docs/prd/kanban-configuravel-prd.md | 1.0 | ok |
| docs/spdd/kanban-configuravel-canvas.md | — | draft |

---

## Evolução do SDD

| Data | Mudança |
|---|---|
| 2026-08-26 | Workspace inicializado via init.py |
| 2026-08-27 | PRD kanban-configuravel v1.0 gerado e validado (18 RFs, 5 RNFs, 17 regras, 5 casos de uso) |

---

### kanban-configuravel
- **Etapa concluída:** /prd (v1.0) — 2026-08-27
- **Artefato:** docs/prd/kanban-configuravel-prd.md
- **RFs Must Have:** RF-001, RF-002, RF-003, RF-004, RF-005, RF-006, RF-007, RF-008, RF-009, RF-010, RF-011, RF-012, RF-013, RF-014, RF-015, RF-016, RF-017, RF-018
- **Questões em aberto:** nenhuma
- **Interface visual detectada:** sim — recomendar /designer antes do /techspec
- **Próximo comando:** /designer kanban-configuravel
