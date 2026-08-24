package com.crudao.kanban.domain.rbac;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record PapelRequest(@NotBlank String nome, @NotNull Set<String> permissoes) {}
