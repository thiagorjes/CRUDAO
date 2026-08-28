package com.crudao.kanban.workflow.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtapaResponse {
    private UUID id;
    private String nome;
    private Integer ordem;
    private Boolean etapaFinal;
    private List<UUID> transicoesSaida;
}

