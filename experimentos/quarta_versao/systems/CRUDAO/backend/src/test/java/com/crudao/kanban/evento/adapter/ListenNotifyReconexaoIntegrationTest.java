package com.crudao.kanban.evento.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.crudao.kanban.evento.EventoBoardPublisher;
import com.crudao.kanban.support.IntegrationTestBase;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * TASK-05.3 — critério de aceite principal: matar a conexão JDBC do listener em execução;
 * ele deve reconectar com backoff e o próximo {@code NOTIFY} ainda propagar via STOMP.
 *
 * <p>Roda contra o PostgreSQL do stack Docker final (profile {@code it}).
 */
@DisplayName("TASK-05.3: reconexão do listener após queda da conexão JDBC")
class ListenNotifyReconexaoIntegrationTest extends IntegrationTestBase {

    @Autowired private ListenNotifyPublisher boardPublisher;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MeterRegistry meterRegistry;

    /** Substitui o broker real para capturar a retransmissão sem subir um cliente STOMP. */
    @MockitoBean private SimpMessagingTemplate messagingTemplate;

    @Test
    @DisplayName("Kill da conexão LISTEN → reconecta e o NOTIFY seguinte chega ao /topic/board")
    void reconectaEContinuaPropagando() {
        await().atMost(Duration.ofSeconds(10)).until(boardPublisher::isConectado);

        // Mata todos os backends do Postgres que estão em LISTEN board_events.
        List<Integer> pids =
                jdbcTemplate.queryForList(
                        "SELECT pid FROM pg_stat_activity "
                                + "WHERE query ILIKE '%LISTEN board_events%' AND pid <> pg_backend_pid()",
                        Integer.class);
        assertThat(pids).isNotEmpty();
        double reconexoesAntes = contarReconexoes();
        pids.forEach(pid -> jdbcTemplate.queryForObject("SELECT pg_terminate_backend(?)", Boolean.class, pid));

        // Reconecta com backoff (primeira tentativa = 1s) e volta a reportar-se saudável.
        await().atMost(Duration.ofSeconds(20)).until(boardPublisher::isConectado);
        await().atMost(Duration.ofSeconds(5)).until(() -> contarReconexoes() > reconexoesAntes);

        UUID projetoId = UUID.randomUUID();
        boardPublisher.publicar(
                new EventoBoardPublisher.EventoBoardPayload("TAREFA_MOVIDA", projetoId, 0L, "{}"));

        verify(messagingTemplate, timeout(5_000))
                .convertAndSend(eq("/topic/board/" + projetoId), (Object) any());
    }

    private double contarReconexoes() {
        var counter =
                meterRegistry.find("kanban.listener.reconnections").tag("canal", "board_events").counter();
        return counter == null ? 0d : counter.count();
    }
}
