package com.crudao.kanban.dashboard;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Lead-time médio agregado do projeto (RF-007). */
@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/projetos/{projetoId}/dashboard")
    public ResponseEntity<DashboardResponse> dashboard(@PathVariable UUID projetoId) {
        return ResponseEntity.ok(dashboardService.dashboard(projetoId));
    }
}
