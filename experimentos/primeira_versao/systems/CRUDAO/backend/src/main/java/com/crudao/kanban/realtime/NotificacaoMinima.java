package com.crudao.kanban.realtime;

import java.util.UUID;

/**
 * Payload mínimo publicado via {@code pg_notify} (ADR-004) — o canal LISTEN/NOTIFY tem limite de
 * 8KB, então carrega apenas IDs; o pod receptor busca o restante do estado antes de retransmitir
 * via STOMP.
 */
public record NotificacaoMinima(TipoEventoBoard tipo, UUID tarefaId, UUID projetoId) {}
