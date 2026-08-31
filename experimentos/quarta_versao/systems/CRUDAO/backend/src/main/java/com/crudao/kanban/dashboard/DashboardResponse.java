package com.crudao.kanban.dashboard;

import java.util.List;
import java.util.UUID;

/**
 * TASK-06.1 / RF-007: Projeção do dashboard de gestão.
 * Lead-time médio por etapa (RN-001) e tempo médio de impedimento agregado por etapa (RN-002).
 */
public record DashboardResponse(
        List<EtapaLeadTime> leadTimeMedioPorEtapa,
        int totalTarefasConsideradas) {

    public record EtapaLeadTime(
            UUID etapaId,
            String etapaNome,
            long leadTimeMedioSegundos,
            long tempoImpedimentoMedioSegundos) {}
}
