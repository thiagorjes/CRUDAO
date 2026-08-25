package com.crudao.kanban.notificacao.adapter;

import com.crudao.kanban.notificacao.NotificacaoPayload;
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
 * Listener por pod do canal {@code notificacao_events} (RF-005), mesmo padrão de {@code
 * BoardEventListener} (ADR-004, TASK-05.1): conexão JDBC dedicada em {@code LISTEN}, retransmitindo
 * cada evento recebido via STOMP a {@code /topic/notificacoes/{usuarioId}}. Reconecta com backoff
 * exponencial (1s→30s) em caso de queda da conexão.
 */
@Component
public class NotificacaoEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoEventListener.class);
    private static final String CANAL = "notificacao_events";
    private static final long BACKOFF_INICIAL_MS = 1000;
    private static final long BACKOFF_MAXIMO_MS = 30_000;

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final String url;
    private final String username;
    private final String password;

    private volatile boolean ativo = true;
    private volatile boolean conectado = false;
    volatile Connection conexaoAtual;
    private Thread thread;

    public NotificacaoEventListener(
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
        thread = new Thread(this::loop, "notificacao-event-listener");
        thread.setDaemon(true);
        thread.start();
    }

    @PreDestroy
    void parar() {
        ativo = false;
        // Fechar a conexão diretamente desbloqueia getNotifications(timeout) de imediato — mesmo
        // achado de code review aplicado em BoardEventListener (TASK-05.1).
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
            NotificacaoPayload payload = objectMapper.readValue(payloadJson, NotificacaoPayload.class);
            messagingTemplate.convertAndSend("/topic/notificacoes/" + payload.usuarioId(), payload);
        } catch (Exception e) {
            log.error("Falha ao processar evento de notificação recebido via NOTIFY: {}", payloadJson, e);
        }
    }

    public boolean isConectado() {
        return conectado;
    }
}
