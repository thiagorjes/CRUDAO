package com.crudao.kanban.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CriarWorkflowRequest {
    @NotBlank(message = "Nome é obrigatório")
    private String nome;
}

