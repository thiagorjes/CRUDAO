package com.crudao.kanban.papel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AtualizarPapelRequest {
    @NotBlank(message = "nome é obrigatório")
    private String nome;
}
