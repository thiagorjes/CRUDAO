package com.crudao.kanban.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Listener LISTEN/NOTIFY dedicado ao canal do dashboard (ADR-004, ADR-005) — mesmo padrão de {@link
 * com.crudao.kanban.realtime.PostgresNotificationListener}, em conexão JDBC própria por pod,
 * retransmitindo via STOMP em {@code /topic/projetos/{projetoId}/dashboard/{jobId}}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DashboardNotificationListener {

  private static final int TIMEOUT_POLL_MS = 1000;

  @Value("${spring.datasource.url}")
  private String url;

  @Value("${spring.datasource.username}")
  private String usuario;

  @Value("${spring.datasource.password}")
  private String senha;

  private final SimpMessagingTemplate messagingTemplate;
  private final ObjectMapper objectMapper;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private volatile boolean ativo = true;

  @EventListener(ApplicationReadyEvent.class)
  public void iniciar() {
    executor.submit(this::loopDeEscuta);
  }

  @PreDestroy
  public void parar() {
    ativo = false;
    executor.shutdownNow();
  }

  private void loopDeEscuta() {
    while (ativo) {
      try (Connection conexao = DriverManager.getConnection(url, usuario, senha)) {
        try (Statement statement = conexao.createStatement()) {
          statement.execute("LISTEN " + DashboardEventoPublisher.CANAL);
        }
        PGConnection pgConexao = conexao.unwrap(PGConnection.class);
        while (ativo && !conexao.isClosed()) {
          PGNotification[] notificacoes = pgConexao.getNotifications(TIMEOUT_POLL_MS);
          if (notificacoes != null) {
            for (PGNotification notificacao : notificacoes) {
              processar(notificacao.getParameter());
            }
          }
        }
      } catch (SQLException e) {
        if (ativo) {
          log.warn("Conexão de LISTEN/NOTIFY do dashboard perdida, tentando reconectar em 1s", e);
          aguardar();
        }
      }
    }
  }

  private void aguardar() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }

  private void processar(String payloadJson) {
    try {
      DashboardResultadoDTO resultado =
          objectMapper.readValue(payloadJson, DashboardResultadoDTO.class);
      messagingTemplate.convertAndSend(
          "/topic/projetos/" + resultado.projetoId() + "/dashboard/" + resultado.jobId(),
          resultado);
    } catch (Exception e) {
      log.error("Falha ao processar notificação de dashboard: {}", payloadJson, e);
    }
  }
}
