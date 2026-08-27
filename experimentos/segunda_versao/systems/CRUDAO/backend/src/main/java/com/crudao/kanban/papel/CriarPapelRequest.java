package com.crudao.kanban.papel;

import jakarta.validation.constraints.NotBlank;

public record CriarPapelRequest(@NotBlank String chave, @NotBlank String nome) {}
