package com.crudao.kanban.tarefa.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarTarefaRequest {

    @NotBlank(message = "Título é obrigatório")
    private String titulo;

    private String descricaoEscopo;

    private UUID responsavelId;

    private UUID raiaId;
}

