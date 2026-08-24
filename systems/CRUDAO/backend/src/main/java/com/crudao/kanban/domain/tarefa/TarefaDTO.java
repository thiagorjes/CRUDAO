package com.crudao.kanban.domain.tarefa;

import java.time.Instant;
import java.util.UUID;

public record TarefaDTO(
    UUID id,
    UUID projetoId,
    UUID workflowId,
    UUID etapaAtualId,
    UUID raiaId,
    TipoTarefa tipo,
    String titulo,
    String descricao,
    UUID responsavelId,
    boolean impedida,
    boolean iniciada,
    Instant criadoEm) {}
