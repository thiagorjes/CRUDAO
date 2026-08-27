package com.crudao.kanban.evento;

import java.util.UUID;

/**
 * Porta de domínio para publicação de eventos de board (ADR-004) — desacoplada do mecanismo de
 * transporte (LISTEN/NOTIFY hoje, Redis/Kafka possível no futuro). Implementada por {@code
 * evento.adapter.ListenNotifyPublisher}.
 *
 * <p>Chamada pelos services de escrita de tarefa (criar/mover/excluir) dentro da mesma transação
 * de escrita — a publicação real só ocorre após o commit ({@code
 * TransactionSynchronization.afterCommit}), nunca antes, para não notificar mudança sujeita a
 * rollback tardio.
 */
public interface EventoBoardPublisher {

    void publicar(UUID projetoId, TipoEventoBoard tipo, UUID tarefaId);
}
