# TASK-05.3 — Frontend: Painel de Administração [G]

**Epic:** EPIC-05 — Frontend Next.js | **User Story:** US-05.1 — Interfaces do sistema
**Sistema:** CRUDAO | **RF:** RF-008, RF-009, RF-010, RF-011, RF-013, RF-015, RF-016 | **Dependências:** TASK-04.2, TASK-01.2, TASK-01.3
**Status:** Concluída — 2026-08-24

---

## Contexto

**Retrabalhada nesta revisão (PRD v1.3, BDR-001):** a versão anterior desta task foi interrompida na implementação — o AC original ("usuário com permissão restrita edita apenas o projeto de origem") não era enforçável, porque o RBAC da TASK-04.1 era só por papel global, sem vínculo usuário↔projeto. TASK-04.2 resolveu isso; esta task agora consome `GET /api/usuarios/me` para decidir o que exibir (gating de UI, nunca fonte de autorização — RNF-003) e os novos endpoints de membros/toggles/finalização.

Painel único separado do board, com seletor de projeto, para gerenciar Projetos, Workflows, Colunas, Raias, Membros do projeto (RF-015) e Papéis/Permissões (visível só a `admin` global); também acessível em modo restrito a partir do board ("Configurações do projeto").

## O que deve ser feito

- [ ] Buscar `GET /api/usuarios/me` ao carregar o painel — usar `admin`/`projetos[].permissoes` só para exibir/habilitar UI (backend revalida tudo — RNF-003)
- [ ] Seletor de projeto: `admin` global vê todos os projetos; demais usuários só os projetos onde têm algum papel (`projetos[]` de `/usuarios/me`)
- [ ] Telas de CRUD de Projeto, Workflow, Etapa, Transição, Raia (mantém RF-008 a RF-011 da versão anterior)
- [ ] Tela de Membros do projeto (RF-015): listar (`GET /membros`) e editar papéis de um usuário (`PUT /membros/{usuarioId}`) — visível a `admin` global ou a quem tem `projeto:gerenciar` no projeto corrente
- [ ] Tela de Toggles do projeto (RF-016): `devPodeExcluirTarefa`, `devPodeEditarTarefaIniciada`, `gestorVeBoard` — visível a quem tem `projeto:gerenciar` no projeto corrente
- [ ] Ação de finalizar/reabrir projeto — visível a `admin` global ou `project_admin` do projeto corrente; projeto finalizado exibe banner de somente-leitura e desabilita as demais telas deste painel para aquele projeto
- [ ] Tela de CRUD de Papéis/Permissões (RF-013) — **visível apenas se `admin === true`** (nunca a `project_admin`, mesmo com `projeto:gerenciar` — `papel:gerenciar` não é atribuível por projeto, ADR-006/TASK-04.2)
- [ ] Acesso restrito ao projeto corrente via "Configurações do projeto" a partir do board (BoardApp já tem seletor de projeto — reaproveitar `crudao_projeto_id` do localStorage)
- [ ] Feedback de erro (modal de confirmação) e sucesso (toast) conforme DDR-003; 403 do backend sempre tratado como erro visível, nunca silenciado

## Guia técnico

- Referência: `docs/design/kanban-configuravel-design-brief.md`, `docs/techspec/kanban-configuravel-techspec.md` (v1.2, seção 4 — contratos de `/usuarios/me`, `/membros`, `/configuracao`, `/finalizar`, `/papeis`)

## Critérios de aceite

- `admin` global alterna entre todos os projetos; usuário com papel só em 1 projeto só vê esse projeto no seletor
- Aba de Papéis/Permissões não aparece para quem não é `admin` global, mesmo sendo `project_admin`
- Toggles e finalização de projeto refletem no comportamento real do sistema (validado contra TASK-01.3/TASK-04.2), não só na UI
- Projeto finalizado bloqueia edição na UI e no backend simultaneamente (teste manual: tentar via chamada direta à API confirma 403/409)
- Exclusões bloqueadas (RN-005) exibem modal de erro claro, orientando migração

---

_Origem: [docs/tasks/kanban-configuravel-tasks.md](../kanban-configuravel-tasks.md)_
