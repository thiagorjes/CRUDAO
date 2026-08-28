package com.crudao.kanban.raia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AtualizarRaiaRequest {
    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotNull(message = "Ordem é obrigatória")
    private Integer ordem;
}

