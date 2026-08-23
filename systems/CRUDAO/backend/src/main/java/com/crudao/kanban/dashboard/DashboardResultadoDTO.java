package com.crudao.kanban.dashboard;

import java.util.Map;
import java.util.UUID;

/**
 * Resultado agregado do dashboard: lead-time médio e tempo médio em impedimento por etapa,
 * chaveados pelo id da etapa (RF-006, RF-007).
 */
public record DashboardResultadoDTO(
    UUID jobId,
    UUID projetoId,
    StatusJobDashboard status,
    Map<UUID, Double> leadTimeMedioPorEtapaSegundos,
    Map<UUID, Double> tempoMedioImpedimentoPorEtapaSegundos) {}
