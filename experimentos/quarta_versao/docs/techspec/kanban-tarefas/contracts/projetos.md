# Contrato — Projetos

_RF-008, RN-015_

## GET /api/projetos

Lista projetos visíveis ao usuário autenticado (via `UsuarioProjetoPapel`).

**Response 200:** `[{ "id", "nome", "status", "descricao" }]`

## POST /api/projetos

Requer `Usuario.adminGlobal=true` (ADR-007) — não há projeto ainda para escopar `projeto:administrar`
via `UsuarioProjetoPapel`. O admin global é provisionado no primeiro login do e-mail configurado em
`kanban.bootstrap.admin-email`; ele passa a configurar os demais usuários/papéis depois.

**Request:** `{ "nome": "string", "descricao": "string?" }`
**Response 201:** `{ "id", "nome", "descricao", "status": "ATIVO" }`

## PUT /api/projetos/{id}

Edita nome/descrição. Requer `projeto:administrar`. Bloqueado se `status=FINALIZADO` (RN-015).

## POST /api/projetos/{id}/finalizar

Requer `projeto:administrar`. Marca `status=FINALIZADO`, `finalizadoEm=now()`.

**Response 200:** `{ "id", "status": "FINALIZADO" }`

## POST /api/projetos/{id}/reabrir

Requer `projeto:administrar`. Marca `status=ATIVO`, `finalizadoEm=null`.

## Erros

| Código | Situação |
|---|---|
| 403 | Sem `projeto:administrar`, ou tentativa de escrita em projeto finalizado (RN-015 — vale até para admin/project_admin) |
| 404 | Projeto inexistente ou sem vínculo do usuário |
| 422 | `nome` vazio |

RF atendido: RF-008.
