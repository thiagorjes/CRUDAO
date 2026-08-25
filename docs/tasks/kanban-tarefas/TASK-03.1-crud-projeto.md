# TASK-03.1 — CRUD de Projeto incl. finalizar/reabrir

**Status: Concluída — 2026-08-25**

**Tamanho:** [M] 4-8h
**Sistema:** CRUDAO
**RF de origem:** RF-008
**Dependências:** TASK-02.2
**Paralelismo:** [P] com TASK-03.2, TASK-03.3

## Contexto

Ciclo de vida do projeto — finalização bloqueia toda escrita subsequente (RN-015), afeta todas as demais epics de domínio.

## O que deve ser feito

- [ ] Implementar `POST/PUT/GET /api/projetos`.
- [ ] Implementar `finalizar`/`reabrir` com checagem `projeto:administrar`.
- [ ] Implementar guard reutilizável "projeto finalizado → somente leitura" a ser usado pelas demais epics de escrita (Epic 04).

## Guia técnico

- `backend/src/main/java/.../projeto/` — controller, service, repository.
- Contrato: `docs/techspec/kanban-tarefas/contracts/projetos.md`.
- `docs/techspec/kanban-tarefas/data-model.md` — seção Projeto (`status`: `ATIVO`|`FINALIZADO`).

## Critérios de aceite

- Projeto finalizado bloqueia toda escrita, inclusive para `admin`/`project_admin` (RN-015).
- Reabertura restaura capacidade de edição.
- Dashboard/leitura permanecem acessíveis com projeto finalizado.
- Ação sem `projeto:administrar` → `403`.
