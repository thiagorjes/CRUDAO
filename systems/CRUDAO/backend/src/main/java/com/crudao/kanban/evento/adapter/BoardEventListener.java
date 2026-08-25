package com.crudao.kanban.evento.adapter;

import com.crudao.kanban.evento.EventoBoardPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Listener por pod do canal {@code board_events} (ADR-004) — conexão JDBC dedicada (fora do pool
 * Hikari, que recicla/fecha conexões ociosas) em {@code LISTEN}, retransmitindo cada evento
 * recebido via STOMP a {@code /topic/board/{projetoId}}.
 *
 * <p>Roda numa thread própria em loop de vida do pod; reconecta com backoff exponencial (1s→30s)
 * em caso de queda da conexão, logando {@code WARN} progressivo (consequência documentada em
 * ADR-004). {@link #isConectado()} alimenta o readiness probe (ver {@code
 * BoardEventListenerHealthIndicator}).
 */
@Component
public class BoardEventListener {

    private static final Logger log = LoggerFactory.getLogger(BoardEventListener.class);
    private static final String CANAL = "board_events";
    private static final long BACKOFF_INICIAL_MS = 1000;
    private static final long BACKOFF_MAXIMO_MS = 30_000;

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final String url;
    private final String username;
    private final String password;

    private volatile boolean ativo = true;
    private volatile boolean conectado = false;
    // Pacote-privado (não private) para permitir simular queda de conexão em teste — ver
    // BoardEventoNotifyIT.
    volatile Connection conexaoAtual;
    private Thread thread;

    public BoardEventListener(
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @PostConstruct
    void iniciar() {
        thread = new Thread(this::loop, "board-event-listener");
        thread.setDaemon(true);
        thread.start();
    }

    @PreDestroy
    void parar() {
        ativo = false;
        // getNotifications(timeout) é I/O de socket bloqueante que não responde a
        // thread.interrupt() — fechar a conexão diretamente é o que de fato desbloqueia a leitura
        // (IOException imediata), em vez de esperar até 5s pelo timeout do poll (achado de code
        // review, agent QA, TASK-05.1).
        Connection conexao = conexaoAtual;
        if (conexao != null) {
            try {
                conexao.close();
            } catch (Exception e) {
                // ignorado — a conexão está sendo descartada de qualquer forma
            }
        }
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void loop() {
        long backoffMs = BACKOFF_INICIAL_MS;
        while (ativo) {
            try (Connection connection = DriverManager.getConnection(url, username, password)) {
                conexaoAtual = connection;
                try (Statement statement = connection.createStatement()) {
                    statement.execute("LISTEN " + CANAL);
                }
                PGConnection pgConnection = connection.unwrap(PGConnection.class);
                conectado = true;
                backoffMs = BACKOFF_INICIAL_MS;
                log.info("Listener LISTEN {} conectado", CANAL);

                while (ativo) {
                    PGNotification[] notificacoes = pgConnection.getNotifications(5000);
                    if (notificacoes != null) {
                        for (PGNotification notificacao : notificacoes) {
                            retransmitir(notificacao.getParameter());
                        }
                    }
                }
            } catch (Exception e) {
                conectado = false;
                conexaoAtual = null;
                if (!ativo) {
                    return;
                }
                log.warn("Listener LISTEN {} desconectado — reconectando em {}ms", CANAL, backoffMs, e);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                backoffMs = Math.min(backoffMs * 2, BACKOFF_MAXIMO_MS);
                if (backoffMs >= BACKOFF_MAXIMO_MS) {
                    log.error("Listener LISTEN {} não reconecta há {}ms — verificar disponibilidade do Postgres", CANAL, backoffMs);
                }
            }
        }
    }

    private void retransmitir(String payloadJson) {
        try {
            EventoBoardPayload payload = objectMapper.readValue(payloadJson, EventoBoardPayload.class);
            messagingTemplate.convertAndSend("/topic/board/" + payload.projetoId(), payload);
        } catch (Exception e) {
            log.error("Falha ao processar evento de board recebido via NOTIFY: {}", payloadJson, e);
        }
    }

    public boolean isConectado() {
        return conectado;
    }
}
