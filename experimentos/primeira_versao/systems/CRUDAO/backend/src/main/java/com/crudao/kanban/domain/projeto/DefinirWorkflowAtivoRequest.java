package com.crudao.kanban.domain.projeto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DefinirWorkflowAtivoRequest(@NotNull UUID workflowId) {}
