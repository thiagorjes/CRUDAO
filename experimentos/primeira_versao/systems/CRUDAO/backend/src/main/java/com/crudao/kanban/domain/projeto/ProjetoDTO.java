package com.crudao.kanban.domain.projeto;

import java.time.Instant;
import java.util.UUID;

public record ProjetoDTO(
    UUID id,
    String nome,
    String descricao,
    UUID workflowAtivoId,
    Instant dataFinalizacao,
    Instant criadoEm) {}
