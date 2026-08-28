package com.crudao.kanban.tarefa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EditarTarefaRequest {

    @NotBlank(message = "Título não pode ser vazio")
    @Size(min = 1, max = 255, message = "Título deve ter entre 1 e 255 caracteres")
    private String titulo;

    @Size(max = 4096, message = "Descrição não pode exceder 4096 caracteres")
    private String descricaoEscopo;

    private UUID responsavelId;
}
