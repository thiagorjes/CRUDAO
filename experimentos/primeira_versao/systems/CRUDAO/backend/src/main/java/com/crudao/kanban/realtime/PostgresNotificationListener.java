package com.crudao.kanban.realtime;

import com.crudao.kanban.domain.tarefa.Observador;
import com.crudao.kanban.domain.tarefa.ObservadorRepository;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Listener LISTEN/NOTIFY do PostgreSQL — um por pod (ADR-004). Mantém uma conexão JDBC dedicada
 * (fora do pool do Hibernate/HikariCP, pois LISTEN precisa permanecer na mesma sessão) e faz
 * polling das notificações recebidas, retransmitindo cada evento via STOMP aos clientes conectados
 * localmente, incluindo observadores da tarefa (RF-005).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PostgresNotificationListener {

  public static final String CANAL = "board_eventos";
  private static final int TIMEOUT_POLL_MS = 1000;

  @Value("${spring.datasource.url}")
  private String url;

  @Value("${spring.datasource.username}")
  private String usuario;

  @Value("${spring.datasource.password}")
  private String senha;

  private final TarefaRepository tarefaRepository;
  private final ObservadorRepository observadorRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final ObjectMapper objectMapper;
  private final PlatformTransactionManager transactionManager;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private volatile boolean ativo = true;
  private TransactionTemplate transactionTemplate;

  @PostConstruct
  void inicializarTransactionTemplate() {
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.transactionTemplate.setReadOnly(true);
  }

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
          statement.execute("LISTEN " + CANAL);
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
          log.warn("Conexão de LISTEN/NOTIFY perdida, tentando reconectar em 1s", e);
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
      NotificacaoMinima notificacao = objectMapper.readValue(payloadJson, NotificacaoMinima.class);
      // Leitura do estado completo em transação própria — a conexão de LISTEN é dedicada e não
      // participa do EntityManager.
      EventoBoardDTO evento = transactionTemplate.execute(status -> montarEvento(notificacao));
      if (evento != null) {
        messagingTemplate.convertAndSend(
            "/topic/projetos/" + evento.projetoId() + "/board", evento);
      }
    } catch (Exception e) {
      log.error("Falha ao processar notificação de board: {}", payloadJson, e);
    }
  }

  private EventoBoardDTO montarEvento(NotificacaoMinima notificacao) {
    Tarefa tarefa = tarefaRepository.findById(notificacao.tarefaId()).orElse(null);
    if (tarefa == null) {
      return null;
    }
    List<UUID> observadorIds =
        observadorRepository.findByTarefaId(tarefa.getId()).stream()
            .map(Observador::getUsuarioId)
            .toList();
    return new EventoBoardDTO(
        notificacao.tipo(),
        tarefa.getId(),
        notificacao.projetoId(),
        tarefa.getEtapaAtual().getId(),
        tarefa.isImpedida(),
        observadorIds);
  }
}
