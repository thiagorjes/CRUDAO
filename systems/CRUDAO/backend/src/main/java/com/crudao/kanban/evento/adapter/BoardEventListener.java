package com.crudao.kanban.evento.adapter;

import com.crudao.kanban.evento.EventoBoardPayload;
import com.crudao.kanban.listener.AbstractPgListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Listener por pod do canal {@code board_events} (ADR-004) — retransmite cada evento recebido via
 * STOMP a {@code /topic/board/{projetoId}}. Reconexão, backoff e métricas Micrometer herdados de
 * {@link AbstractPgListener} (extraído em TASK-05.3 a partir da implementação original de
 * TASK-05.1 — achado de code review, agent QA, duplicação com {@code NotificacaoEventListener}).
 *
 * <p>{@link #isConectado()} alimenta o readiness probe (ver {@code
 * BoardEventListenerHealthIndicator}).
 */
@Component
public class BoardEventListener extends AbstractPgListener {

    private static final Logger log = LoggerFactory.getLogger(BoardEventListener.class);
    private static final String CANAL = "board_events";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public BoardEventListener(
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
            EventoBoardPayload payload = objectMapper.readValue(payloadJson, EventoBoardPayload.class);
            messagingTemplate.convertAndSend("/topic/board/" + payload.projetoId(), payload);
            registrarLatencia(payload.publicadoEmEpochMillis());
        } catch (Exception e) {
            log.error("Falha ao processar evento de board recebido via NOTIFY: {}", payloadJson, e);
        }
    }
}
