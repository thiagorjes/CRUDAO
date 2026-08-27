package com.crudao.kanban.domain.tarefa;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TarefaMoverRequest(@NotNull UUID etapaDestinoId) {}
