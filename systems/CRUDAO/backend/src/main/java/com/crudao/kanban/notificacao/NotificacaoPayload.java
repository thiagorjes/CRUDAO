package com.crudao.kanban.notificacao;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payload do evento de notificação (RF-005), publicado via {@link NotificacaoPublisher} e
 * retransmitido a {@code /topic/notificacoes/{usuarioId}} — mesmo padrão de {@code
 * EventoBoardPayload} (ADR-004), canal próprio.
 *
 * <p>{@code publicadoEmEpochMillis} marca o instante do {@code pg_notify}, usado só no listener
 * para medir latência NOTIFY→broadcast STOMP (TASK-05.3).
 */
public record NotificacaoPayload(
        UUID id,
        UUID usuarioId,
        UUID tarefaId,
        String tipo,
        String mensagem,
        boolean lida,
        OffsetDateTime criadoEm,
        long seq,
        long publicadoEmEpochMillis) {}
