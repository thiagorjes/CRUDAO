package com.crudao.kanban.workflow;

import java.util.List;
import java.util.UUID;

public record TransicoesResponse(UUID etapaId, List<UUID> transicoesSaida) {}
