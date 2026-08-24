package com.crudao.kanban.domain.workflow;

import java.util.UUID;

public record EtapaDTO(UUID id, UUID workflowId, String nome, int ordem, boolean etapaFinal) {}
