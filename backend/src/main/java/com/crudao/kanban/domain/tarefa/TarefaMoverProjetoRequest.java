package com.crudao.kanban.domain.tarefa;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TarefaMoverProjetoRequest(
    @NotNull UUID projetoDestinoId, @NotNull UUID etapaDestinoId) {}
