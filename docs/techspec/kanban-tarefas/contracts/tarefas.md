# Contrato — Tarefas (Cards)

_RF-001, RF-003, RF-004, RF-005, RF-006, RF-012, RF-017, RF-018, RF-019, RN-001, RN-002, RN-004, RN-011, RN-012, RN-013, RN-016, RN-CB-001 a RN-CB-005_

## GET /api/projetos/{projetoId}/board

Retorna o estado completo do board (etapas × raias × cards) para renderização inicial (RF-001).

**Response 200:**
```json
{
  "etapas": [{ "id", "nome", "ordem", "transicoesSaida": ["etapaId"] }],
  "raias": [{ "id", "nome", "ordem" }],
  "tarefas": [{
    "id", "titulo", "etapaAtualId", "raiaId", "responsavelId",
    "impedida", "impedidaDesde", "iniciada"
  }]
}
```

## POST /api/projetos/{projetoId}/tarefas

Criar card pelo board (RF-018). Requer `tarefa:gerenciar`, projeto `ATIVO` (RN-CB-003).

**Request:** `{ "titulo": "string", "descricaoEscopo": "string?", "responsavelId": "uuid?", "raiaId": "uuid?" }`

Se `responsavelId` omitido → card sem responsável (RN-CB-004). Se `raiaId` omitido → primeira raia do projeto ou raia default global (RN-CB-005). Etapa inicial sempre a de menor `ordem` do workflow ativo do projeto.

**Response 201:** `{ "id", "titulo", "etapaAtualId", "raiaId", "responsavelId": null }` — evento `TAREFA_CRIADA` publicado via STOMP (ADR-004).

## GET /api/tarefas/{id}

Detalhe da tarefa (RF-003, TL-04), incluindo lead-time por etapa (RF-006) calculado a partir de `TarefaEtapaHistorico`/`TarefaImpedimentoHistorico`.

**Response 200:** inclui `historicoEtapas: [{ "etapaId", "entradaEm", "saidaEm", "leadTimeSegundos" }]`, `tempoImpedimentoTotalSegundos`.

## PUT /api/tarefas/{id}

Edição de tarefa (RF-003). Campos aceitos dependem de `iniciada`:
- `iniciada=false`: todos os campos (`titulo`, `descricaoEscopo`, `responsavelId`).
- `iniciada=true`: apenas `responsavelId` (regras de atribuição RN-012 abaixo). Envio de `titulo`/`descricaoEscopo` com tarefa iniciada → `409`.

Toda alteração de `responsavelId`/`titulo` gera linha em `TarefaAuditoria` (RN-016).

**Regras de atribuição (RN-012):** dev só pode se autoatribuir (`responsavelId = usuário autenticado`); `product_owner`/`project_admin`/`admin` atribuem/reatribuem livremente a qualquer usuário vinculado ao projeto.

## DELETE /api/tarefas/{id}

Excluir card pelo board (RF-019). Requer `tarefa:gerenciar` e, se o usuário for do papel `dev`, adicionalmente o toggle `devPodeExcluirTarefa` habilitado no projeto (RN-CB-002). Bloqueado se projeto `FINALIZADO` (RN-CB-003).

**Response 204.** Evento `TAREFA_EXCLUIDA` publicado via STOMP em até 2s (RNF-001, ADR-004).

## POST /api/tarefas/{id}/mover

Move a tarefa entre etapas (drag-and-drop do board).

**Request:** `{ "etapaDestinoId": "uuid" }`

Validações, em ordem: (1) transição `etapaAtual → etapaDestino` existe (RF-002); (2) se `etapaDestino.etapaFinal=true`, usuário precisa de `tarefa:finalizar` (RN-011); (3) se `etapaAtual.etapaFinal=true` (ou seja, é uma "desfinalização"), usuário também precisa de `tarefa:finalizar` (RN-004, RN-011, RF-012). Em sucesso: fecha o registro atual em `TarefaEtapaHistorico` (`saidaEm=now()`), abre um novo (`entradaEm=now()`), marca `iniciada=true` se saiu da 1ª etapa, registra `TarefaAuditoria`, publica evento `TAREFA_MOVIDA` (RNF-001) e notifica observadores (RF-005).

**Erro 409:** transição não configurada (RF-002 critério de aceite).
**Erro 403:** falta `tarefa:finalizar` para mover para/desde a etapa final.

## POST /api/tarefas/{id}/impedimento

Marca impedimento (RF-004). Requer `tarefa:impedimento` (default: dev, product_owner, project_admin, admin — RN-013). Abre `TarefaImpedimentoHistorico`, seta `impedida=true`, `impedidaDesde=now()`, notifica observadores.

## DELETE /api/tarefas/{id}/impedimento

Desmarca impedimento. Fecha `TarefaImpedimentoHistorico` (`desmarcadoEm=now()`), `impedida=false`.

## POST /api/tarefas/{id}/observadores

Adiciona observador explícito à tarefa (suporte a RF-005).

**Request:** `{ "usuarioId": "uuid" }`

## GET /api/tarefas/{id}/auditoria

Histórico de auditoria da tarefa (RF-017). Requer papel gestor ou admin no projeto.

`[{ "autorId", "campo", "valorAnterior", "valorNovo", "dataHora" }]`

## Erros gerais

| Código | Situação |
|---|---|
| 403 | Permissão insuficiente (`tarefa:gerenciar`, `tarefa:finalizar`, `tarefa:impedimento` conforme ação) |
| 404 | Tarefa/projeto inexistente |
| 409 | Transição não configurada; projeto/tarefa em estado incompatível (finalizado, campo congelado) |
| 422 | Payload inválido |

RFs atendidos: RF-001, RF-003, RF-004, RF-005, RF-006, RF-012, RF-017, RF-018, RF-019.
