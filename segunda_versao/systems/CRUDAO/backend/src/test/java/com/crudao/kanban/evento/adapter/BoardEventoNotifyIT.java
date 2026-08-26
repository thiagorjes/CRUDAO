package com.crudao.kanban.evento.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.crudao.kanban.evento.EventoBoardPayload;
import com.crudao.kanban.evento.TipoEventoBoard;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Valida o mecanismo LISTEN/NOTIFY fim a fim contra um Postgres real (ADR-004, RNF-001), sem
 * subir o contexto Spring/segurança completo — {@link ListenNotifyPublisher} publica via {@code
 * pg_notify}, {@link BoardEventListener} recebe via {@code LISTEN board_events} e retransmite
 * (aqui, um {@link SimpMessagingTemplate} mockado no lugar do broker STOMP real; a checagem de que
 * o cliente STOMP conectado recebe o frame é do broker Spring, não deste componente).
 *
 * <p>Cobre os critérios de aceite: propagação em <2s (RNF-001, Awaitility), payload com {@code
 * seq} incremental — e, adicionalmente (achado de code review, agent QA, TASK-05.1), a
 * reconexão do listener após queda de conexão (consequência documentada em ADR-004).
 */
@Testcontainers
class BoardEventoNotifyIT {

    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private ObjectMapper objectMapper;
    private ListenNotifyPublisher publisher;
    private BoardEventListener listener;
    private SimpMessagingTemplate messagingTemplate;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        publisher =
                new ListenNotifyPublisher(
                        objectMapper, POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        messagingTemplate = mock(SimpMessagingTemplate.class);
        meterRegistry = new SimpleMeterRegistry();
        listener =
                new BoardEventListener(
                        messagingTemplate,
                        objectMapper,
                        meterRegistry,
                        POSTGRES.getJdbcUrl(),
                        POSTGRES.getUsername(),
                        POSTGRES.getPassword());
        listener.iniciar();
        await().atMost(Duration.ofSeconds(5)).until(listener::isConectado);
    }

    @AfterEach
    void tearDown() {
        listener.parar();
    }

    @Test
    void eventoPublicadoViaNotify_ePropagadoAoTopicoDoBoardEmMenosDe2s() {
        UUID projetoId = UUID.randomUUID();
        UUID tarefaId = UUID.randomUUID();

        publisher.publicar(projetoId, TipoEventoBoard.TAREFA_MOVIDA, tarefaId);

        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                verify(messagingTemplate)
                                        .convertAndSend(
                                                eq("/topic/board/" + projetoId), any(EventoBoardPayload.class)));
    }

    @Test
    void seqIncrementaACadaEventoDoMesmoProjeto() {
        UUID projetoId = UUID.randomUUID();

        publisher.publicar(projetoId, TipoEventoBoard.TAREFA_CRIADA, UUID.randomUUID());
        publisher.publicar(projetoId, TipoEventoBoard.TAREFA_MOVIDA, UUID.randomUUID());

        var payloadCaptor = org.mockito.ArgumentCaptor.forClass(EventoBoardPayload.class);
        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                verify(messagingTemplate, org.mockito.Mockito.times(2))
                                        .convertAndSend(eq("/topic/board/" + projetoId), payloadCaptor.capture()));

        assertThat(payloadCaptor.getAllValues())
                .extracting(EventoBoardPayload::seq)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void reconectaAposQuedaDeConexaoEVoltaAReceberEventos() throws Exception {
        // Simula uma queda de conexão externa (ex.: restart do Postgres) fechando a conexão por
        // baixo do listener — o Postgres em si continua no ar, então a reconexão deve suceder.
        listener.fecharConexaoAtualParaTeste();

        await().atMost(Duration.ofSeconds(2)).until(() -> !listener.isConectado());
        await().atMost(Duration.ofSeconds(10)).until(listener::isConectado);

        UUID projetoId = UUID.randomUUID();
        publisher.publicar(projetoId, TipoEventoBoard.TAREFA_MOVIDA, UUID.randomUUID());

        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                verify(messagingTemplate)
                                        .convertAndSend(
                                                eq("/topic/board/" + projetoId), any(EventoBoardPayload.class)));

        assertThat(meterRegistry.counter("kanban.evento.listener.reconexoes", "canal", "board_events").count())
                .isEqualTo(1.0);
    }

    @Test
    void latenciaNotifyBroadcastEhRegistradaComoMetrica() {
        publisher.publicar(UUID.randomUUID(), TipoEventoBoard.TAREFA_MOVIDA, UUID.randomUUID());

        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                assertThat(
                                                meterRegistry
                                                        .timer("kanban.evento.listener.latencia", "canal", "board_events")
                                                        .count())
                                        .isEqualTo(1L));
    }
}
