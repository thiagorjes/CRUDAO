# TASK-02.1 — Frontend: RBAC gating no BoardApp (permissão e toggle) [P]

**Epic:** EPIC-02 — Frontend: criação e exclusão de card no board
**Sistema:** CRUDAO | **RF:** RF-001, RF-002 | **Dependências:** nenhuma | **[P]** com TASK-01.1

---

## Contexto

`BoardApp` hoje não busca `GET /api/usuarios/me` nem `GET /projetos/{id}/configuracao` — nenhuma ação do board é escondida por permissão até agora. É pré-requisito para as TASK-02.2 (botão "Novo card") e TASK-02.3 (ícone de lixeira), que precisam saber se o usuário tem `tarefa:gerenciar` no projeto e se o toggle `devPodeExcluirTarefa` está habilitado. TechSpec D-05.

## O que deve ser feito

- [ ] `BoardApp` busca `GET /api/usuarios/me` uma vez (ao carregar/trocar de projeto) e calcula `permissoesProjeto` (Set) para o projeto selecionado, reaproveitando exatamente o padrão já usado em `AdminApp`/`MembrosAba`
- [ ] `BoardApp` busca `GET /projetos/{id}/configuracao` para obter `devPodeExcluirTarefa`
- [ ] Calcular e expor (via contexto/props, conforme o padrão já usado no componente) dois booleanos para `CardTarefa` e para o header do board:
  - `podeGerenciarTarefa` — `permissoesProjeto.has('tarefa:gerenciar')`
  - `podeExcluirTarefa` — `podeGerenciarTarefa` e (usuário não é `dev`-tier, ou é `dev`-tier e `devPodeExcluirTarefa === true`)
- [ ] Reaproveitar a heurística `ehDevTier` já extraída na TASK-05.4 (tem `tarefa:gerenciar` mas não `tarefa:atribuir`) — não duplicar a lógica

## Guia técnico

- Arquivos a modificar (paths relativos a `systems/CRUDAO/`):
  - `frontend/src/components/board/BoardApp.tsx`
- Referência de padrão: `frontend/src/components/admin/AdminApp.tsx` (gating via `GET /usuarios/me`), `frontend/src/components/admin/abas/MembrosAba.tsx`
- Tipos já existentes, sem alteração: `UsuarioMe`, `ProjetoPapeis`, `ConfiguracaoProjeto` em `frontend/src/lib/api/types.ts`
- Heurística `ehDevTier`: localizar onde foi extraída na TASK-05.4 (`frontend/src/app/tarefas/[id]/page.tsx` ou lib compartilhada) e reaproveitar/extrair para lib compartilhada se ainda for local à página de detalhe

## Critérios de aceite

- `BoardApp` calcula corretamente `podeGerenciarTarefa`/`podeExcluirTarefa` para os 3 perfis de teste: `project_admin` (ambos `true`), `dev`-tier com toggle habilitado (ambos `true`), `dev`-tier com toggle desabilitado (`podeGerenciarTarefa=true`, `podeExcluirTarefa=false`)
- Usuário sem `tarefa:gerenciar` no projeto: ambos `false`
- Trocar de projeto no seletor do board recalcula `podeGerenciarTarefa`/`podeExcluirTarefa` para o novo projeto, sem exigir reload de página
- Falha ao buscar `GET /projetos/{id}/configuracao` (rede/erro) resulta em `podeExcluirTarefa=false` (fallback seguro, mesmo padrão de `AdminApp`/`TogglesAba`) — nunca expõe a lixeira por erro de fetch
- `tsc --noEmit`, `eslint`, `next build` limpos

---

**Status:** Concluída — 2026-08-24

---

_Origem: [docs/tasks/criacao-card-board-tasks.md](../criacao-card-board-tasks.md)_
