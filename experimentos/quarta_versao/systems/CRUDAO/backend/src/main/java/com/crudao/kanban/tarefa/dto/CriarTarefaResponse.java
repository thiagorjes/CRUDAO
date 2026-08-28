package com.crudao.kanban.tarefa.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarTarefaResponse {

    private UUID id;
    private String titulo;
    private UUID etapaAtualId;
    private UUID raiaId;
    private UUID responsavelId;
}

