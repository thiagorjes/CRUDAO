# TASK-01.2 — Migrations V1-V2: Usuario, Projeto, Papel, Permissao, PapelPermissao, UsuarioProjetoPapel

**Tamanho:** [M] 4-8h
**Sistema:** CRUDAO
**RF de origem:** RF-008, RF-013, RF-014, RF-015, RF-016
**Dependências:** TASK-01.1
**Paralelismo:** nenhum

## Contexto

Base de dados fundacional para autenticação e RBAC — todas as demais entidades referenciam `Usuario` e `Projeto`.

## O que deve ser feito

- [ ] Criar migration V1 (Usuario, Projeto) conforme `docs/techspec/kanban-tarefas/data-model.md`.
- [ ] Criar migration V2 (Papel, Permissao, PapelPermissao, UsuarioProjetoPapel) com seed de papéis (`admin`, `product_owner`, `project_admin`, `dev`, `gestor`), catálogo de permissões (`tarefa:gerenciar`, `tarefa:finalizar`, `tarefa:impedimento`, `projeto:administrar`, `workflow:administrar`, `papel:administrar`, `usuario:associar`) e defaults de `PapelPermissao` refletindo RN-011, RN-012, RN-013, RN-CB-001, RN-CB-002.
- [ ] Criar entidades JPA correspondentes + repositórios Spring Data.
- [ ] Garantir `admin` como papel global (`projetoId=null`) e protegido (RN-006).

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
