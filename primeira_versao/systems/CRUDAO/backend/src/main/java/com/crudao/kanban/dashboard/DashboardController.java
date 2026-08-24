package com.crudao.kanban.dashboard;

import com.crudao.kanban.security.ExigePermissao;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Disparo e consulta do cálculo agregado do dashboard de gestão (RF-007, ADR-005). O cálculo roda
 * em background; o resultado é entregue via STOMP (ver {@link DashboardNotificationListener}), com
 * fallback de polling neste mesmo endpoint de job.
 */
@RestController
@RequestMapping("/api/projetos/{projetoId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;

  @PostMapping("/calcular")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @ExigePermissao("dashboard:visualizar")
  public JobDTO calcular(
      @PathVariable UUID projetoId, @Valid @RequestBody DashboardCalcularRequest request) {
    UUID jobId = dashboardService.solicitarCalculo(projetoId, request);
    dashboardService.calcular(jobId);
    return new JobDTO(jobId);
  }

  @GetMapping("/jobs/{jobId}")
  @ExigePermissao("dashboard:visualizar")
  public DashboardResultadoDTO buscarJob(@PathVariable UUID projetoId, @PathVariable UUID jobId) {
    return dashboardService.buscarResultado(jobId);
  }

  public record JobDTO(UUID jobId) {}
}
