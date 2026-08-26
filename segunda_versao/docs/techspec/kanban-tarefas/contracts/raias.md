# Contrato — Raias (Swimlanes)

_RF-011, RN-CB-005_

Requer `workflow:administrar` e projeto `ATIVO` para escrita; leitura requer apenas vínculo com o projeto.

## GET /api/projetos/{projetoId}/raias

Retorna raias do projeto + raia default global (se o projeto não tiver raias próprias).

`[{ "id", "nome", "ordem", "global": false }]`

## POST /api/projetos/{projetoId}/raias

`{ "nome": "string", "ordem": "int" }` → `201`

## PUT /api/raias/{id}

Edita nome/ordem.

## DELETE /api/raias/{id}

Bloqueado se houver tarefas ativas vinculadas (RN-005) → `409`.

## Erros

| Código | Situação |
|---|---|
| 403 | Sem `workflow:administrar`, ou projeto finalizado |
| 409 | Exclusão com tarefas ativas vinculadas |

RF atendido: RF-011.
