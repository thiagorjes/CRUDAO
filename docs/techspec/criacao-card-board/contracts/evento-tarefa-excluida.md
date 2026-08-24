# Contrato — Evento STOMP `TAREFA_EXCLUIDA`
_Versão: 1.1 | Data: 2026-08-24_

Extensão do tópico já existente `/topic/projetos/{projetoId}/board` (RF-005, TASK-02.2/05.1) — mesma autenticação (`StompAuthChannelInterceptor`), mesmo canal, novo `tipo`. Atende RF-002.

## Publicação (backend)

Disparada por `TarefaService.excluir`, via `publicarAposCommit(TipoEventoBoard.TAREFA_EXCLUIDA, tarefa)` — mesmo padrão de `criar`/`mover`. `excluir` agora faz **soft-delete** (seta `tarefa.excluidaEm` e salva, em vez de `tarefaRepository.delete`) — ver TechSpec §2 D-01. A publicação ocorre em `afterCommit`, quando o `excluidaEm` já está persistido.

## Payload — `pg_notify` (`NotificacaoMinima`)

```json
{ "tipo": "TAREFA_EXCLUIDA", "tarefaId": "<uuid>", "projetoId": "<uuid>" }
```

Sem alteração de estrutura — `NotificacaoMinima` já só carrega esses 3 campos.

## Payload — mensagem STOMP (`EventoBoardDTO`)

```json
{
  "tipo": "TAREFA_EXCLUIDA",
  "tarefaId": "<uuid>",
  "projetoId": "<uuid>",
  "etapaAtualId": "<uuid>",
  "impedida": false,
  "observadorIds": []
}
```

**Sem caso especial no listener:** como a exclusão é soft-delete, a linha da `Tarefa` continua existindo no banco — `PostgresNotificationListener.montarEvento` consulta `TarefaRepository.findById` normalmente para `TAREFA_EXCLUIDA`, exatamente como para `TAREFA_CRIADA`/`TAREFA_MOVIDA`/`IMPEDIMENTO_ALTERADO`. `etapaAtualId` reflete a última etapa em que a tarefa estava; `observadorIds` reflete os observadores no momento da exclusão — nenhum desses campos é relevante para o consumo do frontend (que só usa `tipo`+`tarefaId` para remover o card localmente), mas nenhum precisa ser omitido ou anulado.

## Consumo (frontend)

`conectarBoard` (`frontend/src/lib/board/realtime.ts`) já entrega qualquer `EventoBoard` do tópico ao callback `aoReceberEvento` — nenhuma mudança na função de conexão. Mudança fica em `BoardApp.atualizarTarefaLocal` (`frontend/src/components/board/BoardApp.tsx`): novo branch inicial —

```
if (evento.tipo === 'TAREFA_EXCLUIDA') {
  setEstado((atual) =>
    atual && atual.projeto.id === evento.projetoId
      ? { ...atual, tarefas: atual.tarefas.filter((t) => t.id !== evento.tarefaId) }
      : atual,
  );
  return;
}
```

Mesmo guard de projeto (`evento.projetoId === atual.projeto.id`) já aplicado nos demais tipos, reaproveitado sem alteração.

## Erros

Nenhum — evento é fire-and-forget (mesmo comportamento de `TAREFA_CRIADA`/`TAREFA_MOVIDA`, ADR-004: falha de publicação é logada, não propagada, não desfaz a exclusão já commitada).

## Nota de segurança herdada (não introduzida por este contrato)

O tópico `/topic/projetos/{id}/board` não valida, no CONNECT/SUBSCRIBE, se o usuário é membro do projeto (débito G-RT-01, TASK-04.2/05.1) — qualquer usuário autenticado pode observar `TAREFA_EXCLUIDA` de projetos alheios. Payload minimizado (sem título/descrição) limita o vazamento a "existência/atividade". Não corrigido nesta feature.

## RF atendido

RF-002 (exclusão de card pelo board) — garante que clientes conectados em tempo real vejam o card desaparecer sem precisar recarregar (mesma garantia de RNF-001, <2s, já validada para os demais tipos em `RealtimeBoardIT`).
