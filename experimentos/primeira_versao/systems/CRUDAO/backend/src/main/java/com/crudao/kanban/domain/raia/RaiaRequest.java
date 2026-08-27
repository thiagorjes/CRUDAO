package com.crudao.kanban.domain.raia;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/** {@code projetoId} nulo cria/edita uma raia default global. */
public record RaiaRequest(UUID projetoId, @NotBlank String nome, int ordem) {}
