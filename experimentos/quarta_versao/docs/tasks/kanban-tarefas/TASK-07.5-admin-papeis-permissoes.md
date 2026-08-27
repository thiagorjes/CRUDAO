# TASK-07.5 — Admin: papéis/permissões/usuários

**Tamanho:** [M] 4-8h
**Sistema:** CRUDAO
**RF de origem:** RF-013, RF-015, RF-016
**Dependências:** TASK-07.1, TASK-02.3
**Paralelismo:** [P] com TASK-07.4, TASK-07.6, TASK-07.7
**Status:** Concluída — 2026-08-26

## Contexto

Tela administrativa de RBAC configurável.

## O que deve ser feito

- [x] Tela de gestão de papéis por projeto (exceto `admin`, somente leitura/protegido).
- [x] Tela de toggles de `PapelPermissao`.
- [x] Tela de associação usuário↔projeto↔papel.
- [x] Feedback claro quando bloqueado por RN-017 (autoconcessão).

## Guia técnico

- `frontend/app/projetos/[id]/admin/papeis/`

## Critérios de aceite

- Toggle desabilitado bloqueia ação correspondente na UI e reflete erro real do backend.
- Tentativa de alterar permissão do próprio papel exibe mensagem clara (RN-017).
