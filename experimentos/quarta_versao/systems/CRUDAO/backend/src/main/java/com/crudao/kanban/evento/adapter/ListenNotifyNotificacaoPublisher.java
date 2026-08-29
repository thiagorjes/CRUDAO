package com.crudao.kanban.evento.adapter;

import com.crudao.kanban.evento.NotificacaoEventPublisher;
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
 * Adapter para publicação de eventos de notificação via PostgreSQL LISTEN/NOTIFY.
 *
 * Implementação de {@link NotificacaoEventPublisher} que:
 * 1. Invoca `NOTIFY notificacao_events, '<payload_json>'` após commit da transação.
 * 2. Em background, escuta `LISTEN notificacao_events` em uma thread dedicada.
 * 3. Retransmite eventos recebidos via STOMP a `/topic/notificacoes/{usuarioId}`.
 *
 * ADR-004: Broadcast multi-pod — o `LISTEN` de qualquer pod recebe eventos publicados por qualquer outro pod.
 * TASK-05.2: Suporta notificações por usuário com autorização no backend.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListenNotifyNotificacaoPublisher implements NotificacaoEventPublisher {

    private final DataSource dataSource;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    private volatile Connection listenConnection;
    private volatile boolean listening = false;
    private final AtomicLong sequenceCounter = new AtomicLong(0);
    private Thread listenerThread;

    @PostConstruct
    public void startListener() {
        log.info("Iniciando listener de eventos de notificação LISTEN/NOTIFY");
        listenerThread = new Thread(this::doListen, "notificacao-event-listener");
        listenerThread.setDaemon(false);
        listenerThread.start();
    }

    @PreDestroy
    public void stopListener() {
        log.info("Parando listener de eventos de notificação");
        listening = false;
        if (listenConnection != null) {
            try {
                listenConnection.close();
            } catch (SQLException e) {
                log.warn("Erro ao fechar conexão LISTEN de notificação", e);
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
    public void publicar(NotificacaoEventPayload evento) {
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
     * Executa `NOTIFY notificacao_events, '<payload>'` com a sequência do evento.
     */
    private void publicarViaNotify(NotificacaoEventPayload evento) {
        long seq = sequenceCounter.incrementAndGet();

        try {
            // Serializa evento para JSON
            String payloadJson = objectMapper.writeValueAsString(evento);

            // Limita tamanho do payload (PostgreSQL: 8KB por padrão)
            if (payloadJson.length() > 8000) {
                log.warn("Evento de notificação excede 8KB; será truncado. Tipo: {}, Usuario: {}",
                    evento.tipo(), evento.usuarioId());
                payloadJson = payloadJson.substring(0, 8000);
            }

            // Executa NOTIFY via transação exclusiva
            publicarViaJdbc(payloadJson, seq);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar evento de notificação para JSON", e);
        } catch (SQLException e) {
            log.error("Erro ao executar NOTIFY de notificação", e);
        }
    }

    /**
     * Executa o SQL NOTIFY com a payload do evento.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void publicarViaJdbc(String payloadJson, long seq) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String notifyPayload = String.format("{\"seq\":%d,\"data\":%s}", seq, payloadJson);
            String sanitized = notifyPayload.replace("\\", "\\\\").replace("'", "''");

            if (sanitized.contains("\0")) {
                log.warn("Payload contém null byte — rejeitado");
                return;
            }

            String sql = String.format("NOTIFY notificacao_events, E'%s'", sanitized);
            stmt.execute(sql);
            log.debug("NOTIFY de notificação publicado com seq={}, tipo={}", seq, extractTipo(payloadJson));
        }
    }

    /**
     * Thread principal que escuta eventos via `LISTEN`.
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
                    stmt.execute("LISTEN notificacao_events");
                }

                listening = true;
                log.info("Listener de notificação conectado e escutando notificacao_events");

                org.postgresql.PGNotification[] notifications;
                while (listening) {
                    notifications = pgConn.getNotifications(100);
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
                    log.warn("Erro na conexão LISTEN de notificação (tentativa {}/{}). Reconectando em {}ms",
                        reconnectAttempts, maxReconnectAttempts, delayMs, e);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("Falha ao reconectar listener de notificação após {} tentativas. Desistindo.",
                        maxReconnectAttempts, e);
                    break;
                }
            } finally {
                listening = false;
                if (listenConnection != null) {
                    try {
                        listenConnection.close();
                    } catch (SQLException e) {
                        log.debug("Erro ao fechar conexão LISTEN de notificação", e);
                    }
                }
            }
        }

        log.info("Listener thread de notificação finalizada");
    }

    /**
     * Processa uma notificação recebida do PostgreSQL e a retransmite via STOMP.
     */
    private void processarNotificacao(org.postgresql.PGNotification notif) {
        try {
            String payload = notif.getParameter();
            log.debug("Notificação recebida: {}", payload.substring(0, Math.min(100, payload.length())));

            // Parse JSON para extrair seq e usuarioId
            var node = objectMapper.readTree(payload);
            long seq = node.get("seq").asLong();
            var dataNode = node.get("data");
            String tipo = dataNode.get("tipo").asText();
            String usuarioIdStr = dataNode.get("usuarioId").asText();
            UUID usuarioId = UUID.fromString(usuarioIdStr);

            // Retransmite via STOMP para usuário específico
            String destination = String.format("/topic/notificacoes/%s", usuarioId);
            messagingTemplate.convertAndSend(destination, node);

            log.debug("Evento de notificação retransmitido via STOMP. Tipo: {}, Usuario: {}, Seq: {}",
                tipo, usuarioId, seq);
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
            // Aceita tanto o envelope {"seq":..,"data":{..}} quanto o payload cru {"tipo":..}.
            var dataNode = node.get("data");
            var alvo = dataNode != null ? dataNode : node;
            var tipoNode = alvo.get("tipo");
            return tipoNode != null ? tipoNode.asText("DESCONHECIDO") : "DESCONHECIDO";
        } catch (JsonProcessingException e) {
            return "ERRO";
        }
    }
}
