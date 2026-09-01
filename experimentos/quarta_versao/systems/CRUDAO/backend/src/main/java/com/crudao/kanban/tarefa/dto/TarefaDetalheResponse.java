package com.crudao.kanban.tarefa.dto;

import java.time.Instant;
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
    private String etapaAtualNome;
    private UUID raiaId;
    private String raiaNome;
    private UUID responsavelId;
    private String responsavelNome;
    private boolean iniciada;
    private boolean impedida;
    private Instant impedidaDesde;
    private Instant criadoEm;
    private UUID criadoPorId;
    private String criadoPorNome;
    private List<HistoricoEtapaDTO> historicoEtapas;
    private long tempoImpedimentoTotalSegundos;
    private List<ObservadorDTO> observadores;

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

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ObservadorDTO {
        private UUID id;
        private String nome;
    }
}
