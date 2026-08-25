package com.crudao.kanban.evento;

import java.util.UUID;

/**
 * Payload enxuto do evento de board (ADR-004) — ids + tipo + {@code seq}, para respeitar o limite
 * de 8KB do {@code NOTIFY}. O cliente busca detalhes via {@code GET /api/tarefas/{id}} ou {@code
 * GET /api/projetos/{projetoId}/board} se necessário.
 */
public record EventoBoardPayload(TipoEventoBoard tipo, UUID projetoId, UUID tarefaId, long seq) {}
