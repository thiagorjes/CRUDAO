package com.crudao.kanban.evento;

import java.util.UUID;

/**
 * Payload enxuto do evento de board (ADR-004) — ids + tipo + {@code seq}, para respeitar o limite
 * de 8KB do {@code NOTIFY}. O cliente busca detalhes via {@code GET /api/tarefas/{id}} ou {@code
 * GET /api/projetos/{projetoId}/board} se necessário.
 *
 * <p>{@code publicadoEmEpochMillis} marca o instante do {@code pg_notify} (não o de criação do
 * evento de domínio) — usado só no lado do listener para medir a latência NOTIFY→broadcast STOMP
 * (TASK-05.3, RNF-001/002); não faz parte do contrato consumido pelo frontend.
 */
public record EventoBoardPayload(
        TipoEventoBoard tipo, UUID projetoId, UUID tarefaId, long seq, long publicadoEmEpochMillis) {}
