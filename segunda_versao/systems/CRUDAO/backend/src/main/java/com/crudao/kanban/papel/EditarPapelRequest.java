package com.crudao.kanban.papel;

import jakarta.validation.constraints.NotBlank;

public record EditarPapelRequest(@NotBlank String nome) {}
