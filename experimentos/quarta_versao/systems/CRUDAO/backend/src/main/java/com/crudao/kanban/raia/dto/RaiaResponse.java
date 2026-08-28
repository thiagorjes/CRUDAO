package com.crudao.kanban.raia.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RaiaResponse {
    private UUID id;
    private String nome;
    private Integer ordem;
    private Boolean global;
}

