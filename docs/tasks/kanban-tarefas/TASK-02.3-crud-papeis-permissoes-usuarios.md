# TASK-02.3 — CRUD de papéis/permissões/usuários (RN-006, RN-017)

**Tamanho:** [M] 4-8h
**Sistema:** CRUDAO
**RF de origem:** RF-013, RF-015, RF-016
**Dependências:** TASK-02.2
**Paralelismo:** [P] com TASK-03.1, TASK-03.2, TASK-03.3, TASK-04.x (não bloqueia mais o board — só bloqueia TASK-07.5)

## Contexto

Task destacada de TASK-02.2 (decisão do Comitê de Análise) para não bloquear o paralelismo de 03.1/03.2/03.3, que só dependem do motor de permissões (02.2), não do CRUD administrativo. É a **dona única da migration V8** (`PapelPermissaoAuditoria`) — TASK-04.4 e TASK-06.1 apenas referenciam essa migration já existente, nunca a criam.

## O que deve ser feito

- [ ] Implementar CRUD de papéis por projeto (exceto `admin`, protegido — RN-006), `PapelPermissao` (toggles), associação usuário↔projeto↔papel (RF-015).
- [ ] Implementar RN-017: bloquear alteração de `PapelPermissao` do(s) papel(is) que o próprio usuário possui no projeto — exige outro usuário com `papel:administrar`.
- [ ] Criar migration V8 (`PapelPermissaoAuditoria`) — única dona desta migration no plano; TASK-04.4 e TASK-06.1 apenas a referenciam.
- [ ] Registrar `PapelPermissaoAuditoria` em toda alteração de toggle.

## Guia técnico

- `backend/src/main/java/.../papel/` — CRUD de papéis, toggles, associação.
- `backend/src/main/resources/db/migration/V8__papel_permissao_auditoria.sql`
- Contrato: `docs/techspec/kanban-tarefas/contracts/papeis-permissoes.md`.
- `docs/techspec/kanban-tarefas/data-model.md` — seção PapelPermissaoAuditoria.

## Critérios de aceite

- Tentativa de alterar `PapelPermissao` do próprio papel retorna `403` (RN-017); outro admin consegue.
- Alteração de toggle gera linha em `PapelPermissaoAuditoria`.
- Tentativa de editar/excluir papel `admin` bloqueada (RN-006).
- `UsuarioProjetoPapel` criado via este CRUD reflete corretamente nas permissões efetivas resolvidas por TASK-02.2.
