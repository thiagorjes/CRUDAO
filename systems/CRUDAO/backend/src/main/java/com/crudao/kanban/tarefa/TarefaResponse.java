package com.crudao.kanban.tarefa;

import java.util.UUID;

public record TarefaResponse(UUID id, String titulo, UUID etapaAtualId, UUID raiaId, UUID responsavelId) {}
