package com.crudao.kanban.evento.adapter;

import com.crudao.kanban.evento.EventoBoardPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Adapter para publicação de eventos via PostgreSQL LISTEN/NOTIFY.
 *
 * Implementação de {@link EventoBoardPublisher} que:
 * 1. Invoca `NOTIFY board_events, '<payload_json>'` após commit da transação (via {@link TransactionSynchronization.afterCommit}).
 * 2. Em background, escuta `LISTEN board_events` em uma thread dedicada.
 * 3. Retransmite eventos recebidos via STOMP a `/topic/board/{projetoId}`.
 *
 * ADR-004: Broadcast multi-pod — o `LISTEN` de qualquer pod recebe eventos publicados por qualquer outro pod.
 * ADR-002: Usa DataSource injetado (connection pool) para respeitar pooling e escalabilidade.
 *
 * Trade-offs:
 * - Payload limitado a 8KB por PostgreSQL (mitigado: eventos comprimidos/estruturados).
 * - Sem garantia de replay — eventos perdidos durante reconexão (mitigado: cliente detecta gap e refaz GET /board).
 * - Thread dedicada por pod (escalável até ~100 pods com margem; além disso, considerar broker dedicado).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListenNotifyPublisher implements EventoBoardPublisher {

    private final DataSource dataSource;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    private volatile Connection listenConnection;
    private volatile boolean listening = false;
    private final AtomicLong sequenceCounter = new AtomicLong(0);
    private Thread listenerThread;

    @PostConstruct
    public void startListener() {
        log.info("Iniciando listener de eventos LISTEN/NOTIFY");
        listenerThread = new Thread(this::doListen, "board-event-listener");
        listenerThread.setDaemon(false);
        listenerThread.start();
    }

    @PreDestroy
    public void stopListener() {
        log.info("Parando listener de eventos");
        listening = false;
        if (listenConnection != null) {
            try {
                listenConnection.close();
            } catch (SQLException e) {
                log.warn("Erro ao fechar conexão LISTEN", e);
            }
        }
        if (listenerThread != null) {
            try {
                listenerThread.join(5000); // Aguarda até 5s
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Publica um evento via `NOTIFY` após o commit da transação atual.
     * Se não há transação ativa, publica imediatamente.
     */
    @Override
    public void publicar(EventoBoardPayload evento) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // Registra callback para executar após commit
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        publicarViaNotify(evento);
                    }
                }
            );
        } else {
            // Sem transação ativa, publica imediatamente
            publicarViaNotify(evento);
        }
    }

    /**
     * Executa `NOTIFY board_events, '<payload>'` com a sequência do evento.
     * Separado de {@link #publicar} para clareza de intenção.
     */
    private void publicarViaNotify(EventoBoardPayload evento) {
        long seq = sequenceCounter.incrementAndGet();

        try {
            // Serializa evento para JSON
            String payloadJson = objectMapper.writeValueAsString(evento);

            // Limita tamanho do payload (PostgreSQL: 8KB por padrão)
            if (payloadJson.length() > 8000) {
                log.warn("Evento de board excede 8KB; será truncado. Tipo: {}, Projeto: {}",
                    evento.tipo(), evento.projetoId());
                payloadJson = payloadJson.substring(0, 8000);
            }

            // Executa NOTIFY via transação exclusiva
            publicarViaJdbc(payloadJson, seq);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar evento para JSON", e);
        } catch (SQLException e) {
            log.error("Erro ao executar NOTIFY de evento", e);
        }
    }

    /**
     * Executa o SQL NOTIFY com a payload do evento.
     * Usa uma transação separada para garantir que o NOTIFY é enviado mesmo se houver erro posterior.
     * Sanitiza a entrada de payload para evitar SQL injection (embora NOTIFY não suporte prepared statements).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void publicarViaJdbc(String payloadJson, long seq) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Formata o payload JSON para incluir a sequência com sanitização de entrada
            String notifyPayload = String.format("{\"seq\":%d,\"data\":%s}", seq, payloadJson);
            // Escape de single quotes (padrão PostgreSQL) + validação básica
            String sanitized = notifyPayload.replace("\\", "\\\\").replace("'", "''");

            // Valida que o payload não contém caracteres de controle perigosos
            if (sanitized.contains("\0")) {
                log.warn("Payload contém null byte — rejeitado");
                return;
            }

            String sql = String.format("NOTIFY board_events, E'%s'", sanitized);
            stmt.execute(sql);
            log.debug("NOTIFY publicado com seq={}, tipo={}", seq, extractTipo(payloadJson));
        }
    }

    /**
     * Thread principal que escuta eventos via `LISTEN`.
     * Executa em loop até que {@link #listening} seja setado para false.
     */
    private void doListen() {
        int reconnectAttempts = 0;
        final int maxReconnectAttempts = 10;
        final long reconnectDelayMs = 1000;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                reconnectAttempts = 0; // Reset após conexão bem-sucedida

                listenConnection = dataSource.getConnection();

                PGConnection pgConn = listenConnection.unwrap(PGConnection.class);
                try (Statement stmt = listenConnection.createStatement()) {
                    stmt.execute("LISTEN board_events");
                }

                listening = true;
                log.info("Listener conectado e escutando board_events");

                // Aguarda notificações indefinidamente
                org.postgresql.PGNotification[] notifications;
                while (listening) {
                    notifications = pgConn.getNotifications(100); // timeout 100ms
                    if (notifications != null) {
                        for (org.postgresql.PGNotification notif : notifications) {
                            processarNotificacao(notif);
                        }
                    }
                }
            } catch (SQLException e) {
                if (reconnectAttempts < maxReconnectAttempts) {
                    reconnectAttempts++;
                    long delayMs = reconnectDelayMs * reconnectAttempts;
                    log.warn("Erro na conexão LISTEN (tentativa {}/{}). Reconectando em {}ms",
                        reconnectAttempts, maxReconnectAttempts, delayMs, e);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("Falha ao reconectar listener após {} tentativas. Desistindo.",
                        maxReconnectAttempts, e);
                    break;
                }
            } finally {
                listening = false;
                if (listenConnection != null) {
                    try {
                        listenConnection.close();
                    } catch (SQLException e) {
                        log.debug("Erro ao fechar conexão LISTEN", e);
                    }
                }
            }
        }

        log.info("Listener thread finalizada");
    }

    /**
     * Processa uma notificação recebida do PostgreSQL e a retransmite via STOMP.
     */
    private void processarNotificacao(org.postgresql.PGNotification notif) {
        try {
            String payload = notif.getParameter();
            log.debug("Notificação recebida: {}", payload.substring(0, Math.min(100, payload.length())));

            // Parse JSON para extrair seq e projetoId
            var node = objectMapper.readTree(payload);
            long seq = node.get("seq").asLong();
            var dataNode = node.get("data");
            String tipo = dataNode.get("tipo").asText();
            String projetoIdStr = dataNode.get("projetoId").asText();
            UUID projetoId = UUID.fromString(projetoIdStr);

            // Retransmite via STOMP
            String destination = String.format("/topic/board/%s", projetoId);
            messagingTemplate.convertAndSend(destination, node);

            log.debug("Evento retransmitido via STOMP. Tipo: {}, Projeto: {}, Seq: {}",
                tipo, projetoId, seq);
        } catch (Exception e) {
            log.error("Erro ao processar notificação", e);
        }
    }

    /**
     * Extrai o campo `tipo` de um JSON para logging.
     */
    private String extractTipo(String payloadJson) {
        try {
            var node = objectMapper.readTree(payloadJson);
            return node.get("tipo").asText("DESCONHECIDO");
        } catch (JsonProcessingException e) {
            return "ERRO";
        }
    }
}
