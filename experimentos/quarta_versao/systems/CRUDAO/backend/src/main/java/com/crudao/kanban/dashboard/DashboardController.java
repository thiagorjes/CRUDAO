package com.crudao.kanban.dashboard;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * TASK-06.1 / RF-007: Endpoint do dashboard de gestão.
 * Contrato: docs/techspec/kanban-tarefas/contracts/dashboard-notificacoes.md
 */
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/projetos/{projetoId}/dashboard")
    public DashboardResponse obter(@PathVariable UUID projetoId) {
        return dashboardService.obterDashboard(projetoId);
    }
}
