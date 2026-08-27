# TASK-01.2 — Migrations V1-V2: Usuario, Projeto, Papel, Permissao, PapelPermissao, UsuarioProjetoPapel

**Status:** Concluída — 2026-08-25
**Tamanho:** [M] 4-8h
**Sistema:** CRUDAO
**RF de origem:** RF-008, RF-013, RF-014, RF-015, RF-016
**Dependências:** TASK-01.1
**Paralelismo:** nenhum

## Contexto

Base de dados fundacional para autenticação e RBAC — todas as demais entidades referenciam `Usuario` e `Projeto`.

## O que deve ser feito

- [x] Criar migration V1 (Usuario, Projeto) conforme `docs/techspec/kanban-tarefas/data-model.md`.
- [x] Criar migration V2 (Papel, Permissao, PapelPermissao, UsuarioProjetoPapel) com catálogo de permissões (8 chaves, incl. `tarefa:excluir` — decisão do Comitê em TASK-04.4) e seed do papel `admin` (global, protegido, com todas as permissões habilitadas).
  - **Decisão tomada com o usuário:** papéis default por projeto (`product_owner`, `project_admin`, `dev`, `gestor`) **não** são semeados em V2 — são escopados por `Projeto` (que ainda não existe nesta migration). Ficam como constante no código, instanciados pelo `ProjetoService` ao criar um projeto (TASK-03.1), aplicando os defaults de RN-011/012/013/RN-CB-001/002 nesse momento.
- [x] Criar entidades JPA correspondentes + repositórios Spring Data.
- [x] Garantir `admin` como papel global (`projetoId=null`) e protegido (RN-006) — reforçado por índice único parcial `uk_papel_global_chave`.

## Guia técnico

- `backend/src/main/resources/db/migration/V1__usuario_projeto.sql`
- `backend/src/main/resources/db/migration/V2__papel_permissao.sql`
- `backend/src/main/java/.../domain/usuario/`
- `backend/src/main/java/.../domain/papel/`

Referência de campos: `docs/techspec/kanban-tarefas/data-model.md` (seções Usuario, Projeto, Papel, Permissao, PapelPermissao, UsuarioProjetoPapel).

## Critérios de aceite

- Flyway aplica V1/V2 sem erro no boot (Testcontainers).
- Seed de papéis/permissões presente e defaults batem com RN-011/012/013/RN-CB-001/002 (teste de integração lendo `PapelPermissao`).
- Índices `UNIQUE(keycloakSub)`, `UNIQUE(email)` em `Usuario`, `UNIQUE(projetoId, chave)` em `Papel` presentes.
