package com.crudao.kanban.domain.raia;

import java.util.UUID;

public record RaiaDTO(UUID id, UUID projetoId, String nome, int ordem) {}
