package com.crudao.kanban.tarefa;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Detalhe da tarefa (RF-003, TL-04), incluindo lead-time por etapa (RF-006). */
public record TarefaDetalheResponse(
        UUID id,
        String titulo,
        String descricaoEscopo,
        UUID etapaAtualId,
        UUID raiaId,
        UUID responsavelId,
        boolean iniciada,
        boolean impedida,
        OffsetDateTime impedidaDesde,
        List<HistoricoEtapaResponse> historicoEtapas,
        long tempoImpedimentoTotalSegundos) {}
