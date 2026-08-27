# TASK-05.1 — EventoBoardPublisher + adapter LISTEN/NOTIFY + STOMP + autorização de subscrição

**Tamanho:** [G] 1-2 dias
**Sistema:** CRUDAO
**RF de origem:** RNF-001, RNF-002 (suporte a RF-002/RF-005/RF-019)
**Dependências:** TASK-04.1, TASK-04.2, TASK-04.4, TASK-04.5
**Paralelismo:** nenhum

## Contexto

Mecanismo de broadcast multi-pod ([ADR-004](../../decisions/ADR-004-broadcast-listen-notify.md)) — viabiliza atualização em tempo real do board sem depender de refresh manual.

## O que deve ser feito

- [ ] Definir porta de domínio `EventoBoardPublisher` (interface) desacoplada do mecanismo de transporte.
- [ ] Implementar adapter LISTEN/NOTIFY: `NOTIFY board_events, '<payload_json + seq>'`, invocado via `TransactionSynchronization.afterCommit` nos services de Epic 04 (mover, criar, excluir).
- [ ] Implementar listener por pod (`LISTEN board_events`) retransmitindo via STOMP a `/topic/board/{projetoId}`.
- [ ] Implementar `ChannelInterceptor` validando autorização na subscrição STOMP: vínculo `UsuarioProjetoPapel` ativo com o `projetoId` (board) — rejeita com `ERROR` STOMP caso contrário.
- [ ] Conectar os pontos de publicação já criados em TASK-04.2 (mover), TASK-04.1 (criar) e TASK-04.4 (excluir) ao publisher. **Nota:** esta task altera código já implementado nessas três (injeta o publisher nos services existentes) — considerar no esforço, não é só código novo isolado.

## Guia técnico

- `backend/src/main/java/.../evento/EventoBoardPublisher.java` (porta de domínio)
- `backend/src/main/java/.../evento/adapter/ListenNotifyPublisher.java`
- `backend/src/main/java/.../websocket/StompConfig.java`
- `backend/src/main/java/.../websocket/BoardChannelInterceptor.java`
- Referência: [ADR-004](../../decisions/ADR-004-broadcast-listen-notify.md), TechSpec Seção 5.

## Critérios de aceite

- Movimentação/criação/exclusão de card propaga evento STOMP a `/topic/board/{projetoId}` em <2s (RNF-001, validado com `Awaitility`).
- Subscrição sem vínculo ao projeto rejeitada com `ERROR` STOMP.
- Evento publicado é recebido por cliente conectado (validação com 1 pod; teste multi-pod completo em TASK-08.1).
- Payload do evento inclui `seq` incremental (base para resincronização em TASK-05.3).

---

**Status:** Concluída — 2026-08-25
