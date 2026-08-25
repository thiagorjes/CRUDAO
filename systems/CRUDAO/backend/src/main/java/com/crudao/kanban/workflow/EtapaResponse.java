package com.crudao.kanban.workflow;

import java.util.List;
import java.util.UUID;

public record EtapaResponse(UUID id, String nome, int ordem, boolean etapaFinal, List<UUID> transicoesSaida) {}
