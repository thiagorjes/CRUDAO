package com.crudao.kanban.workflow;

import java.util.List;
import java.util.UUID;

public record WorkflowComEtapasResponse(UUID id, String nome, List<EtapaResponse> etapas) {}
