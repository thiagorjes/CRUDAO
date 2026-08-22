# TASK-05.3 — Frontend: Painel de Administração [G]

**Epic:** EPIC-05 — Frontend Next.js | **User Story:** US-05.1 — Interfaces do sistema
**Sistema:** CRUDAO | **RF:** RF-008, RF-009, RF-010, RF-011, RF-013 | **Dependências:** TASK-04.1, TASK-01.2

---

## Contexto

Painel único separado do board, com seletor de projeto, para gerenciar Projetos, Workflows, Colunas, Raias e Papéis/Permissões; também acessível em modo restrito a partir do board ("Configurações do projeto").

## O que deve ser feito

- [ ] Implementar painel administrativo com seletor de projeto (troca de contexto)
- [ ] Telas de CRUD de Projeto, Workflow, Etapa, Transição, Raia
- [ ] Tela de CRUD de Papéis/Permissões (visível apenas a quem tem a permissão correspondente)
- [ ] Acesso restrito ao projeto corrente via "Configurações do projeto" a partir do board
- [ ] Feedback de erro (modal de confirmação) e sucesso (toast) conforme DDR-003

## Guia técnico

- Referência: `docs/design/kanban-configuravel-design-brief.md`

## Critérios de aceite

- Admin global alterna entre projetos no painel; usuário com permissão restrita edita apenas o projeto de origem
- Exclusões bloqueadas (RN-005) exibem modal de erro claro, orientando migração

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
