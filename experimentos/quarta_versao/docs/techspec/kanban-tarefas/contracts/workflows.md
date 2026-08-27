# Contrato — Workflows, Etapas e Transições

_RF-002, RF-009, RF-010, RN-003, RN-004, RN-005_

Todos os endpoints requerem `workflow:administrar` no projeto e projeto `ATIVO` (RN-015).

## GET /api/projetos/{projetoId}/workflows

`[{ "id", "nome", "etapas": [{ "id", "nome", "ordem", "etapaFinal", "transicoesSaida": ["etapaDestinoId", ...] }] }]`

## POST /api/projetos/{projetoId}/workflows

`{ "nome": "string" }` → `201 { "id", "nome" }`

## DELETE /api/workflows/{id}

Bloqueado se houver tarefas ativas vinculadas a qualquer etapa do workflow (RN-005) → `409`.

## POST /api/workflows/{id}/etapas

**Request:** `{ "nome": "string", "ordem": "int", "etapaFinal": "boolean" }`

Validação: se `etapaFinal=false`, a etapa deve ter ao menos uma transição de saída configurada antes de ser considerada válida para uso em produção — o sistema permite salvar sem transição, mas bloqueia o board de exibir a etapa como operacional até haver ≥1 transição (RN-003; o `PUT` de transições injeta a exigência).

## PUT /api/etapas/{id}

Edita nome/ordem/etapaFinal. Reordenar dispara recalculo de `ordem` das demais etapas do workflow.

## DELETE /api/etapas/{id}

Bloqueado se houver tarefas ativas na etapa (RN-005) → `409`.

## PUT /api/etapas/{id}/transicoes

**Request:** `{ "etapasDestinoIds": ["uuid", ...] }` — substitui o conjunto de transições de saída da etapa.

**Response 200:** `{ "etapaId", "transicoesSaida": [...] }`
**Erro 422:** etapa não-final com `transicoesSaida` vazio (RN-003).

## Erros comuns

| Código | Situação |
|---|---|
| 403 | Sem `workflow:administrar`, ou projeto finalizado |
| 409 | Exclusão de workflow/etapa com tarefas ativas vinculadas (RN-005) |
| 422 | Etapa não-final sem transição de saída (RN-003) |

RFs atendidos: RF-002, RF-009, RF-010.
