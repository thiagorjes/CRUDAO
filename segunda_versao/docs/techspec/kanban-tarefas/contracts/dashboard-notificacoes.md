# Contrato — Dashboard e Notificações

_RF-005, RF-006, RF-007_

## GET /api/projetos/{projetoId}/dashboard

Lead-time médio agregado (RF-007). Acessível a qualquer usuário vinculado ao projeto (inclusive papel gestor, sem permissão de execução).

**Response 200:**
```json
{
  "leadTimeMedioPorEtapa": [{ "etapaId", "etapaNome", "leadTimeMedioSegundos", "tempoImpedimentoMedioSegundos" }],
  "totalTarefasConsideradas": "int"
}
```

Calculado a partir de `TarefaEtapaHistorico`/`TarefaImpedimentoHistorico` agregados no backend (query nativa/JPQL com `AVG`); sem pré-cálculo/materialização nesta fase (volume esperado não justifica).

Se `projeto.status=FINALIZADO`: endpoint permanece acessível normalmente (RN-015 — somente leitura, não bloqueia leitura).

## GET /api/notificacoes

Lista notificações do usuário autenticado (RF-005), mais recentes primeiro.

**Query params:** `?apenasNaoLidas=true`

**Response 200:** `[{ "id", "tarefaId", "tipo", "mensagem", "lida", "criadoEm" }]`

## POST /api/notificacoes/{id}/lida

Marca notificação como lida.

## WebSocket — /topic/notificacoes/{usuarioId}

Canal STOMP para push de notificações em tempo real, complementar ao GET (RF-005, RNF-001). Payload igual ao item da lista acima, incluindo `seq` (contador por canal, usado para resincronização — ADR-004).

**Autorização na subscrição (achado do Comitê de Análise — Security/Architect):** `SUBSCRIBE` é validado em `ChannelInterceptor` — só é aceito se `usuarioId` do tópico == principal autenticado do JWT da sessão WebSocket. Subscrição de `usuarioId` de terceiros é rejeitada com `ERROR` STOMP (cobre RNF-003 para o canal WebSocket, não só REST).

## WebSocket — /topic/board/{projetoId}

Canal STOMP para eventos de board (RNF-001, ADR-004): `TAREFA_CRIADA`, `TAREFA_EXCLUIDA`, `TAREFA_MOVIDA`, `TAREFA_IMPEDIMENTO_ALTERADO`. Payload enxuto (ids + tipo + `seq`) — cliente busca detalhes via REST quando necessário (limite de 8KB do `NOTIFY`, ADR-004). Ao detectar gap de `seq` ou reconexão, o cliente deve refazer `GET /api/projetos/{projetoId}/board` para resincronizar.

**Autorização na subscrição:** `SUBSCRIBE` validado em `ChannelInterceptor` — exige vínculo `UsuarioProjetoPapel` ativo do usuário autenticado com o `projetoId` do tópico. Sem vínculo → `ERROR` STOMP, sem entrega de nenhum evento do board.

## Erros

| Código | Situação |
|---|---|
| 403 | Usuário sem vínculo ao projeto tentando acessar dashboard/board |
| 404 | Notificação inexistente ou de outro usuário |

RFs atendidos: RF-005, RF-006, RF-007.
