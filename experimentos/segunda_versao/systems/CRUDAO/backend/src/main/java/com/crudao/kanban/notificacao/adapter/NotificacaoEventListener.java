package com.crudao.kanban.notificacao.adapter;

import com.crudao.kanban.listener.AbstractPgListener;
import com.crudao.kanban.notificacao.NotificacaoPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Listener por pod do canal {@code notificacao_events} (RF-005) — retransmite cada evento
 * recebido via STOMP a {@code /topic/notificacoes/{usuarioId}}. Reconexão, backoff e métricas
 * Micrometer herdados de {@link AbstractPgListener}, mesmo padrão de {@code BoardEventListener}
 * (ADR-004, TASK-05.1; extração para a base comum em TASK-05.3).
 *
 * <p>{@link #isConectado()} alimenta o readiness probe (ver {@code
 * NotificacaoEventListenerHealthIndicator}).
 */
@Component
public class NotificacaoEventListener extends AbstractPgListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoEventListener.class);
    private static final String CANAL = "notificacao_events";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public NotificacaoEventListener(
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        super(log, CANAL, meterRegistry, url, username, password);
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void processarNotificacao(String payloadJson) {
        try {
            NotificacaoPayload payload = objectMapper.readValue(payloadJson, NotificacaoPayload.class);
            messagingTemplate.convertAndSend("/topic/notificacoes/" + payload.usuarioId(), payload);
            registrarLatencia(payload.publicadoEmEpochMillis());
        } catch (Exception e) {
            log.error("Falha ao processar evento de notificação recebido via NOTIFY: {}", payloadJson, e);
        }
    }
}
