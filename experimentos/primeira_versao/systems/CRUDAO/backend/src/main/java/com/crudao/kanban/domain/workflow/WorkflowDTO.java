package com.crudao.kanban.domain.workflow;

import java.util.UUID;

public record WorkflowDTO(UUID id, UUID projetoId, String nome, int versao) {}
