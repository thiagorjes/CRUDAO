package com.crudao.kanban.tarefa.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarefaDetalheResponse {
    private UUID id;
    private String titulo;
    private String descricaoEscopo;
    private UUID etapaAtualId;
    private UUID raiaId;
    private UUID responsavelId;
    private boolean iniciada;
    private boolean impedida;
    private List<HistoricoEtapaDTO> historicoEtapas;
    private long tempoImpedimentoTotalSegundos;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricoEtapaDTO {
        private UUID etapaId;
        private String etapaNome;
        private long leadTimeSegundos;
    }
}
