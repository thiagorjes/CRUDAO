package com.crudao.kanban.domain.workflow;

import java.util.UUID;

public record TransicaoDTO(UUID id, UUID etapaOrigemId, UUID etapaDestinoId, TipoTransicao tipo) {}
