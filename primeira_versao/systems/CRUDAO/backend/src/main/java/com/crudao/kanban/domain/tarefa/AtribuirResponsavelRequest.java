package com.crudao.kanban.domain.tarefa;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** RN-012: atribuir/autoatribuir ("puxar") a responsabilidade por uma tarefa. */
public record AtribuirResponsavelRequest(@NotNull UUID usuarioId) {}
