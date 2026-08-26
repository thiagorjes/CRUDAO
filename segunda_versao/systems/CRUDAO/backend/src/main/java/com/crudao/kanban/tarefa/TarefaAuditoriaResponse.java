package com.crudao.kanban.tarefa;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Item do histórico de auditoria da tarefa (RF-017, {@code GET /api/tarefas/{id}/auditoria}). */
public record TarefaAuditoriaResponse(
        UUID autorId, String campo, String valorAnterior, String valorNovo, OffsetDateTime dataHora) {}
