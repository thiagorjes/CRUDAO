package com.crudao.kanban.dashboard;

import java.util.List;

/** Resposta do dashboard de lead-time do projeto (RF-007). */
public record DashboardResponse(
        List<DashboardEtapaResponse> leadTimeMedioPorEtapa, int totalTarefasConsideradas) {}
