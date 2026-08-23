package com.crudao.kanban.domain.workflow;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TransicaoRequest(
    @NotNull UUID etapaOrigemId, @NotNull UUID etapaDestinoId, @NotNull TipoTransicao tipo) {}
