package com.crudao.kanban.evento.adapter;

import com.crudao.kanban.evento.EventoBoardPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import javax.sql.DataSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Adapter de {@link EventoBoardPublisher} sobre PostgreSQL LISTEN/NOTIFY (canal {@code board_events}),
 * retransmitindo para {@code /topic/board/{projetoId}}.
 *
 * <p>Toda a infraestrutura de publicação, escuta, reconexão resiliente (TASK-05.3) e métricas
 * está em {@link AbstractListenNotifyRelay}.
 */
@Service
public class ListenNotifyPublisher extends AbstractListenNotifyRelay<EventoBoardPublisher.EventoBoardPayload>
        implements EventoBoardPublisher {

    public ListenNotifyPublisher(
            DataSource dataSource,
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        super(dataSource, messagingTemplate, objectMapper, meterRegistry);
    }

    @Override
    protected String canal() {
        return "board_events";
    }

    @Override
    public void publicar(EventoBoardPayload evento) {
        super.publicar(evento);
    }

    @Override
    protected String serializarPayload(EventoBoardPayload evento) throws JsonProcessingException {
        return objectMapper.writeValueAsString(evento);
    }

    @Override
    protected String destinoStomp(JsonNode data) {
        return "/topic/board/" + data.path("projetoId").asText();
    }

    @Override
    protected String payloadResync(EventoBoardPayload evento) {
        return String.format(
                "{\"tipo\":\"%s\",\"projetoId\":\"%s\",\"truncado\":true}",
                evento.tipo(), evento.projetoId());
    }
}
