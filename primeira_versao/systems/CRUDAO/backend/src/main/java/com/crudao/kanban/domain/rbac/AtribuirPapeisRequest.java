package com.crudao.kanban.domain.rbac;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** RF-015 — papéis do catálogo (já existentes) a atribuir a um usuário num projeto. */
public record AtribuirPapeisRequest(@NotNull List<UUID> papeis) {}
