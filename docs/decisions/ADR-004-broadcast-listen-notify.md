# ADR-004 — Broadcast de eventos multi-pod via PostgreSQL LISTEN/NOTIFY

_Status: Aceito | Data: 2026-08-25 | Feature: kanban-tarefas_

## Contexto

RNF-001 exige propagação de alterações do board (movimentação, criação/exclusão de card, impedimento) para os demais usuários conectados em até 2s. RNF-002 exige que o sistema opere com 1 pod e escale para 2+ pods sem divergência de estado. `architecture.md` já apontava PostgreSQL `LISTEN/NOTIFY` como candidato, condicionado a não introduzir cache/broker dedicado nesta fase (ADR-002).

## Decisão

Broadcast de eventos em tempo real entre pods via PostgreSQL `LISTEN/NOTIFY`:
- Cada pod mantém uma conexão JDBC dedicada em `LISTEN` no canal `board_events`.
- Ao persistir uma mudança de estado relevante (movimentação de card, criação/exclusão, impedimento marcado/desmarcado), o backend executa `NOTIFY board_events, '<payload_json>'` na mesma transação.
- O pod que recebe o `NOTIFY` retransmite o evento aos clientes STOMP inscritos no tópico do projeto/board correspondente (`/topic/board/{projetoId}`), incluindo o próprio pod que originou a alteração (para evitar lógica duplicada de "não notificar quem originou").
- Publicação do `NOTIFY` é desacoplada da camada de Service via uma porta de domínio `EventoBoardPublisher`, implementada por um adapter LISTEN/NOTIFY e invocada em `TransactionSynchronization.afterCommit` — nunca dentro da transação de escrita, evitando `NOTIFY` de mudança que sofre rollback tardio e mantendo a Service livre de detalhe de infraestrutura (facilita troca futura para Redis/Kafka).
- Cada pod mantém um contador de sequência (`seq`) por `projetoId`, incrementado a cada evento publicado e incluído no payload do `NOTIFY`. O frontend guarda o último `seq` recebido por board; ao detectar um gap (seq recebido > último + 1) ou ao reconectar o WebSocket, dispara automaticamente um `GET /api/projetos/{projetoId}/board` para resincronizar — rede de segurança client-side contra perda de evento durante reconexão do listener (sem isso, RNF-002 fica exposto a divergência silenciosa entre pods).

## Alternativas consideradas

| Opção | Prós | Contras |
|---|---|---|
| **PostgreSQL LISTEN/NOTIFY (escolhida)** | Sem infraestrutura adicional; consistente com ADR-002; usa a fonte única de verdade (Postgres) | Payload limitado a 8KB por notificação; sem persistência/replay de eventos perdidos durante desconexão momentânea do listener |
| Redis Pub/Sub | Desacoplado, sem limite de payload, padrão de mercado para broadcast multi-instância | Introduz componente de infraestrutura novo, contradiz ADR-002 nesta fase |
| Broadcast só em memória (sem Postgres) | Mais simples em single-pod | Quebra RNF-002 diretamente — eventos não propagam entre pods |

## Consequências

- Payload do `NOTIFY` deve ser mantido enxuto (ids + tipo de evento + `seq`; o cliente busca detalhes via REST se necessário) para respeitar o limite de 8KB.
- Reconexão do listener JDBC após queda de conexão é tratada com retry/backoff no backend, com liveness/readiness do pod refletindo o estado da conexão (Actuator) e log `WARN`→`ERROR` progressivo se as tentativas se esgotarem (detalhar em task de infraestrutura de eventos).
- Resincronização client-side por `seq`/reconexão (ver Decisão) mitiga o risco de divergência de estado entre pods sob RNF-002, mesmo sem garantia de entrega do `NOTIFY`.
- Métricas mínimas via Micrometer/Actuator: contador de reconexões do listener por pod, latência `NOTIFY`→broadcast STOMP — sem introduzir stack de tracing completa (`observability.md` mantém "sem APM nesta fase").
- Roteamento de notificação por usuário (`/topic/notificacoes/{usuarioId}`) em ambiente multi-pod exige `UserDestinationMessageHandler`/broker relay compartilhado ou resolução própria apoiada no mesmo canal `board_events` — sem isso, `convertAndSendToUser` isolado por pod não garante entrega quando o usuário está conectado a um pod diferente do que processou o evento. Detalhar o mecanismo escolhido na task de implementação do canal de notificações.
- Reavaliar para Redis/Kafka se o volume de eventos ou o número de pods crescer significativamente além do previsto (dezenas a centenas de usuários simultâneos, conforme RNF-002).

## Refinamentos (Comitê de Análise Assíncrono, 2026-08-25)

Os pontos acima (porta `EventoBoardPublisher`, resincronização por `seq`, roteamento multi-pod por usuário, observabilidade mínima) foram incorporados após revisão dos agents Architect e DevOps no `/techspec` — ver `kanban-tarefas-techspec.md` Seções 5 e 8.

## Referências

RF-005, RF-019, RNF-001, RNF-002, ADR-002.
