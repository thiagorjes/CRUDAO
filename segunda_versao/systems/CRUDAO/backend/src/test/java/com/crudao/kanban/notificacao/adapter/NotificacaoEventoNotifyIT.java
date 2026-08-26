package com.crudao.kanban.notificacao.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.crudao.kanban.domain.notificacao.Notificacao;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.notificacao.NotificacaoPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Valida o mecanismo LISTEN/NOTIFY do canal {@code notificacao_events} fim a fim contra um
 * Postgres real (RF-005) — mesmo padrão de {@code BoardEventoNotifyIT} (board, TASK-05.1),
 * espelhado aqui em TASK-05.3 (achado de code review, agent QA: cobertura assimétrica entre os
 * dois canais). Entidades {@link Usuario}/{@link Tarefa} são criadas só com id (sem persistir) —
 * {@link ListenNotifyNotificacaoPublisher#publicar} só lê os ids via getters, não aciona lazy
 * loading.
 */
@Testcontainers
class NotificacaoEventoNotifyIT {

    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private ObjectMapper objectMapper;
    private ListenNotifyNotificacaoPublisher publisher;
    private NotificacaoEventListener listener;
    private SimpMessagingTemplate messagingTemplate;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        publisher =
                new ListenNotifyNotificacaoPublisher(
                        objectMapper, POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        messagingTemplate = mock(SimpMessagingTemplate.class);
        meterRegistry = new SimpleMeterRegistry();
        listener =
                new NotificacaoEventListener(
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

    private Notificacao notificacao(UUID usuarioId) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        Tarefa tarefa = new Tarefa();
        tarefa.setId(UUID.randomUUID());

        Notificacao notificacao = new Notificacao();
        notificacao.setId(UUID.randomUUID());
        notificacao.setUsuario(usuario);
        notificacao.setTarefa(tarefa);
        notificacao.setTipo("TRANSICAO_ETAPA");
        notificacao.setMensagem("Tarefa movida");
        notificacao.setLida(false);
        notificacao.setCriadoEm(OffsetDateTime.now());
        return notificacao;
    }

    @Test
    void eventoPublicadoViaNotify_ePropagadoAoTopicoDoUsuarioEmMenosDe2s() {
        UUID usuarioId = UUID.randomUUID();

        publisher.publicar(notificacao(usuarioId));

        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                verify(messagingTemplate)
                                        .convertAndSend(
                                                eq("/topic/notificacoes/" + usuarioId), any(NotificacaoPayload.class)));

        assertThat(meterRegistry.timer("kanban.evento.listener.latencia", "canal", "notificacao_events").count())
                .isEqualTo(1L);
    }

    @Test
    void seqIncrementaACadaEventoDoMesmoUsuario() {
        UUID usuarioId = UUID.randomUUID();

        publisher.publicar(notificacao(usuarioId));
        publisher.publicar(notificacao(usuarioId));

        var payloadCaptor = org.mockito.ArgumentCaptor.forClass(NotificacaoPayload.class);
        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                verify(messagingTemplate, org.mockito.Mockito.times(2))
                                        .convertAndSend(
                                                eq("/topic/notificacoes/" + usuarioId), payloadCaptor.capture()));

        assertThat(payloadCaptor.getAllValues())
                .extracting(NotificacaoPayload::seq)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void reconectaAposQuedaDeConexaoEVoltaAReceberEventos() {
        listener.fecharConexaoAtualParaTeste();

        await().atMost(Duration.ofSeconds(2)).until(() -> !listener.isConectado());
        await().atMost(Duration.ofSeconds(10)).until(listener::isConectado);

        UUID usuarioId = UUID.randomUUID();
        publisher.publicar(notificacao(usuarioId));

        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                verify(messagingTemplate)
                                        .convertAndSend(
                                                eq("/topic/notificacoes/" + usuarioId), any(NotificacaoPayload.class)));

        assertThat(meterRegistry.counter("kanban.evento.listener.reconexoes", "canal", "notificacao_events").count())
                .isEqualTo(1.0);
    }
}
