package com.crudao.kanban.domain.projeto;

import jakarta.validation.constraints.NotBlank;

public record ProjetoRequest(@NotBlank String nome, String descricao) {}
