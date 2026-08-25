package com.crudao.kanban.papel;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssociarUsuarioRequest(@NotNull UUID usuarioId, @NotNull UUID papelId) {}
