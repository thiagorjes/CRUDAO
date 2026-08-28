package com.crudao.kanban.tarefa.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Resposta do board (GET /api/projetos/{projetoId}/board).
 * RF-001: Retorna etapas (ordenadas), raias e tarefas agrupadas.
 * Projetado sem N+1 via JPQL SELECT NEW.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponse {
    private List<EtapaCardDTO> etapas;
    private List<RaiaCardDTO> raias;
    private List<TarefaCardDTO> tarefas;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EtapaCardDTO {
        private UUID id;
        private String nome;
        private int ordem;
        private List<UUID> transicoesSaida;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RaiaCardDTO {
        private UUID id;
        private String nome;
        private int ordem;
        private boolean global;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TarefaCardDTO {
        private UUID id;
        private String titulo;
        private UUID etapaAtualId;
        private UUID raiaId;
        private UUID responsavelId;
        private boolean impedida;
        private long impedidaDesdeMs;
        private boolean iniciada;
    }
}
