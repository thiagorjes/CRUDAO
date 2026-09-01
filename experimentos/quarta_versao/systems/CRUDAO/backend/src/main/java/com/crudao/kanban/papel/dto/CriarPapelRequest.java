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
public class CriarPapelRequest {
    @NotBlank(message = "chave é obrigatória")
    private String chave;

    @NotBlank(message = "nome é obrigatório")
    private String nome;
}
