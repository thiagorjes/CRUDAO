package com.crudao.kanban.dashboard;

import com.crudao.kanban.common.RecursoNaoEncontradoException;
import com.crudao.kanban.domain.leadtime.RegistroEtapa;
import com.crudao.kanban.domain.leadtime.RegistroEtapaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cálculo agregado do dashboard de gestão: lead-time médio por etapa e tempo médio em impedimento
 * por etapa, filtrado por período (RF-007). Disparado de forma assíncrona (ADR-005) para não
 * bloquear a requisição HTTP inicial em agregações sobre período longo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

  private final DashboardJobRepository dashboardJobRepository;
  private final RegistroEtapaRepository registroEtapaRepository;
  private final DashboardEventoPublisher dashboardEventoPublisher;
  private final ObjectMapper objectMapper;

  @Transactional
  public UUID solicitarCalculo(UUID projetoId, DashboardCalcularRequest request) {
    DashboardJob job = new DashboardJob();
    job.setProjetoId(projetoId);
    job.setDataInicio(request.dataInicio());
    job.setDataFim(request.dataFim());
    job.setStatus(StatusJobDashboard.PROCESSANDO);
    DashboardJob salvo = dashboardJobRepository.save(job);
    return salvo.getId();
  }

  /**
   * Executa o cálculo em background. Roda em transação/thread própria — chamado a partir do
   * controller, depois que o job já foi persistido e o {@code jobId} devolvido ao cliente (202).
   */
  @Async("dashboardExecutor")
  @Transactional
  public void calcular(UUID jobId) {
    DashboardJob job =
        dashboardJobRepository
            .findById(jobId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Job não encontrado: " + jobId));
    try {
      List<RegistroEtapa> registros =
          registroEtapaRepository.findByTarefaProjetoIdAndSaidaEmIsNotNullAndEntradaEmBetween(
              job.getProjetoId(), job.getDataInicio(), job.getDataFim());

      Map<UUID, Double> leadTimeMedioPorEtapa =
          registros.stream()
              .collect(
                  Collectors.groupingBy(
                      r -> r.getEtapa().getId(), Collectors.averagingLong(this::leadTimeSegundos)));

      Map<UUID, Double> tempoMedioImpedimentoPorEtapa =
          registros.stream()
              .collect(
                  Collectors.groupingBy(
                      r -> r.getEtapa().getId(),
                      Collectors.averagingLong(RegistroEtapa::getTempoImpedimentoSegundos)));

      DashboardResultadoDTO resultado =
          new DashboardResultadoDTO(
              jobId,
              job.getProjetoId(),
              StatusJobDashboard.CONCLUIDO,
              leadTimeMedioPorEtapa,
              tempoMedioImpedimentoPorEtapa);

      job.setStatus(StatusJobDashboard.CONCLUIDO);
      job.setResultadoJson(objectMapper.writeValueAsString(resultado));
      job.setConcluidoEm(Instant.now());
      dashboardJobRepository.save(job);
      dashboardEventoPublisher.publicar(resultado);
    } catch (Exception e) {
      log.error("Falha ao calcular dashboard (jobId={})", jobId, e);
      job.setStatus(StatusJobDashboard.ERRO);
      job.setConcluidoEm(Instant.now());
      dashboardJobRepository.save(job);
    }
  }

  @Transactional(readOnly = true)
  public DashboardResultadoDTO buscarResultado(UUID jobId) {
    DashboardJob job =
        dashboardJobRepository
            .findById(jobId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Job não encontrado: " + jobId));
    if (job.getResultadoJson() == null) {
      return new DashboardResultadoDTO(
          job.getId(), job.getProjetoId(), job.getStatus(), Map.of(), Map.of());
    }
    try {
      return objectMapper.readValue(job.getResultadoJson(), DashboardResultadoDTO.class);
    } catch (Exception e) {
      throw new IllegalStateException("Falha ao ler resultado do job " + jobId, e);
    }
  }

  private long leadTimeSegundos(RegistroEtapa registro) {
    return Duration.between(registro.getEntradaEm(), registro.getSaidaEm()).getSeconds();
  }
}
