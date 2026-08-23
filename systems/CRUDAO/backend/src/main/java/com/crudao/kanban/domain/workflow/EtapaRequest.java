package com.crudao.kanban.domain.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EtapaRequest(
    @NotNull UUID workflowId, @NotBlank String nome, int ordem, boolean etapaFinal) {}
