package com.crudao.kanban.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica o resultado do dashboard via {@code pg_notify} em canal dedicado (ADR-004, ADR-005) — ao
 * contrário do canal de board, carrega o resultado agregado completo (mapas por etapa), pois o
 * objetivo é evitar uma segunda consulta ao banco pelo pod receptor; o payload é pequeno o
 * suficiente para caber no limite de 8KB do LISTEN/NOTIFY.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardEventoPublisher {

  public static final String CANAL = "dashboard_eventos";

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public void publicar(DashboardResultadoDTO resultado) {
    try {
      String payload = objectMapper.writeValueAsString(resultado);
      jdbcTemplate.execute(
          (ConnectionCallback<Void>)
              conexao -> {
                try (PreparedStatement statement =
                    conexao.prepareStatement("SELECT pg_notify(?, ?)")) {
                  statement.setString(1, CANAL);
                  statement.setString(2, payload);
                  statement.execute();
                }
                return null;
              });
    } catch (Exception e) {
      log.error(
          "Falha ao publicar resultado do dashboard (jobId={}) — clientes conectados não serão"
              + " notificados; polling em GET .../jobs/{} ainda funciona.",
          resultado.jobId(),
          resultado.jobId(),
          e);
    }
  }
}
