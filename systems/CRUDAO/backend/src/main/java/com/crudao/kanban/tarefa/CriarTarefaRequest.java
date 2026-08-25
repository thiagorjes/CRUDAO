package com.crudao.kanban.tarefa;

import java.util.UUID;

public record CriarTarefaRequest(
        String titulo, String descricaoEscopo, UUID responsavelId, UUID raiaId) {}
