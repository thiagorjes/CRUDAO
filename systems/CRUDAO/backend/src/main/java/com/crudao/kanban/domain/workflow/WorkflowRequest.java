package com.crudao.kanban.domain.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record WorkflowRequest(@NotNull UUID projetoId, @NotBlank String nome) {}
