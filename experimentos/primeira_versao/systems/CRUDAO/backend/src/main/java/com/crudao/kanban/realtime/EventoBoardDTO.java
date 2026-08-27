package com.crudao.kanban.realtime;

import java.util.List;
import java.util.UUID;

/**
 * Payload completo entregue aos clientes via STOMP no tópico {@code /topic/projetos/{id}/board}
 * (RF-005). Inclui os observadores da tarefa para que o frontend possa notificá-los.
 */
public record EventoBoardDTO(
    TipoEventoBoard tipo,
    UUID tarefaId,
    UUID projetoId,
    UUID etapaAtualId,
    boolean impedida,
    List<UUID> observadorIds) {}
