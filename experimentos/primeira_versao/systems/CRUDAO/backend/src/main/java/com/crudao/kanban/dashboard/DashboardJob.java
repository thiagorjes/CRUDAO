package com.crudao.kanban.dashboard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Job de cálculo assíncrono do dashboard (RF-007, ADR-005). Persistido no PostgreSQL (fonte única
 * de estado, ADR-002) em vez de mantido em memória, para que o polling de {@code GET
 * .../jobs/{jobId}} funcione independente de qual pod atendeu a requisição.
 *
 * <p>{@code resultadoJson} carrega o {@link DashboardResultadoDTO} serializado — mapas por etapa
 * não têm cardinalidade previsível para modelar como colunas.
 */
@Entity
@Table(name = "dashboard_job")
@Getter
@Setter
@NoArgsConstructor
public class DashboardJob {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "projeto_id", nullable = false)
  private UUID projetoId;

  @Enumerated(EnumType.STRING)
  private StatusJobDashboard status = StatusJobDashboard.PROCESSANDO;

  private Instant dataInicio;

  private Instant dataFim;

  @Lob private String resultadoJson;

  private Instant criadoEm = Instant.now();

  private Instant concluidoEm;
}
