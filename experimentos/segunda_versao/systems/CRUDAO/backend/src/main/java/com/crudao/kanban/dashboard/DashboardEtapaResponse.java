package com.crudao.kanban.dashboard;

import java.util.UUID;

/** Agregado de lead-time e impedimento de uma etapa (RF-007). */
public record DashboardEtapaResponse(
        UUID etapaId, String etapaNome, long leadTimeMedioSegundos, long tempoImpedimentoMedioSegundos) {}
