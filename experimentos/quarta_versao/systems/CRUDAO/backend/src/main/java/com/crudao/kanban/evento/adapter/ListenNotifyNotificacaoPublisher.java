package com.crudao.kanban.evento.adapter;

import com.crudao.kanban.evento.NotificacaoEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import javax.sql.DataSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Adapter de {@link NotificacaoEventPublisher} sobre PostgreSQL LISTEN/NOTIFY
 * (canal {@code notificacao_events}), retransmitindo para {@code /topic/notificacoes/{usuarioId}}.
 *
 * <p>Toda a infraestrutura de publicação, escuta, reconexão resiliente (TASK-05.3) e métricas
 * está em {@link AbstractListenNotifyRelay}.
 */
@Service
public class ListenNotifyNotificacaoPublisher
        extends AbstractListenNotifyRelay<NotificacaoEventPublisher.NotificacaoEventPayload>
        implements NotificacaoEventPublisher {

    public ListenNotifyNotificacaoPublisher(
            DataSource dataSource,
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        super(dataSource, messagingTemplate, objectMapper, meterRegistry);
    }

    @Override
    protected String canal() {
        return "notificacao_events";
    }

    @Override
    public void publicar(NotificacaoEventPayload evento) {
        super.publicar(evento);
    }

    @Override
    protected String serializarPayload(NotificacaoEventPayload evento) throws JsonProcessingException {
        return objectMapper.writeValueAsString(evento);
    }

    @Override
    protected String destinoStomp(JsonNode data) {
        return "/topic/notificacoes/" + data.path("usuarioId").asText();
    }

    @Override
    protected String payloadResync(NotificacaoEventPayload evento) {
        return String.format(
                "{\"tipo\":\"%s\",\"usuarioId\":\"%s\",\"truncado\":true}",
                evento.tipo(), evento.usuarioId());
    }
}
