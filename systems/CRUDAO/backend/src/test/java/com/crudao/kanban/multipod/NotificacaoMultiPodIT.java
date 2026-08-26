package com.crudao.kanban.multipod;

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
import com.crudao.kanban.notificacao.adapter.ListenNotifyNotificacaoPublisher;
import com.crudao.kanban.notificacao.adapter.NotificacaoEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Valida escalabilidade horizontal do canal {@code notificacao_events} (RF-005 sob RNF-002,
 * TASK-08.1) — mesmo padrão de {@link BoardMultiPodIT}: uma {@link Notificacao} gerada e publicada
 * no pod A deve chegar ao cliente STOMP conectado ao pod B em {@code /topic/notificacoes/{usuarioId}},
 * ambos os pods compartilhando o mesmo Postgres.
 */
@Testcontainers
class NotificacaoMultiPodIT {

    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private ObjectMapper objectMapper;
    private ListenNotifyNotificacaoPublisher publisher;
    private NotificacaoEventListener listenerPodA;
    private NotificacaoEventListener listenerPodB;
    private SimpMessagingTemplate templatePodA;
    private SimpMessagingTemplate templatePodB;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        publisher =
                new ListenNotifyNotificacaoPublisher(
                        objectMapper, POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());

        templatePodA = mock(SimpMessagingTemplate.class);
        listenerPodA =
                new NotificacaoEventListener(
                        templatePodA,
                        objectMapper,
                        new SimpleMeterRegistry(),
                        POSTGRES.getJdbcUrl(),
                        POSTGRES.getUsername(),
                        POSTGRES.getPassword());

        templatePodB = mock(SimpMessagingTemplate.class);
        listenerPodB =
                new NotificacaoEventListener(
                        templatePodB,
                        objectMapper,
                        new SimpleMeterRegistry(),
                        POSTGRES.getJdbcUrl(),
                        POSTGRES.getUsername(),
                        POSTGRES.getPassword());

        listenerPodA.iniciar();
        listenerPodB.iniciar();
        await().atMost(Duration.ofSeconds(5)).until(listenerPodA::isConectado);
        await().atMost(Duration.ofSeconds(5)).until(listenerPodB::isConectado);
    }

    @AfterEach
    void tearDown() {
        listenerPodA.parar();
        listenerPodB.parar();
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
    void notificacaoGeradaNoPodA_ChegaAoClienteConectadoNoPodBEmMenosDe2s() {
        UUID usuarioId = UUID.randomUUID();

        publisher.publicar(notificacao(usuarioId));

        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () -> {
                            verify(templatePodA)
                                    .convertAndSend(
                                            eq("/topic/notificacoes/" + usuarioId), any(NotificacaoPayload.class));
                            verify(templatePodB)
                                    .convertAndSend(
                                            eq("/topic/notificacoes/" + usuarioId), any(NotificacaoPayload.class));
                        });
    }

    @Test
    void seqIncrementaPorPublisher_ReconexaoDoPodNaoPerdeEventosSubsequentes() {
        // Mesmo padrão de BoardMultiPodIT: seq é contado em memória por instância de publisher
        // (um por pod de escrita), não há sequência global entre pods — a resincronização
        // client-side não depende disso. Aqui validamos o outro lado do canal: o pod B reconecta
        // após queda e volta a receber notificações do mesmo publisher, sem perder o `seq`.
        UUID usuarioId = UUID.randomUUID();

        publisher.publicar(notificacao(usuarioId));
        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                verify(templatePodB)
                                        .convertAndSend(
                                                eq("/topic/notificacoes/" + usuarioId), any(NotificacaoPayload.class)));

        listenerPodB.fecharConexaoAtualParaTeste();
        await().atMost(Duration.ofSeconds(2)).until(() -> !listenerPodB.isConectado());
        await().atMost(Duration.ofSeconds(10)).until(listenerPodB::isConectado);

        publisher.publicar(notificacao(usuarioId));

        ArgumentCaptor<NotificacaoPayload> captorB = ArgumentCaptor.forClass(NotificacaoPayload.class);
        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                verify(templatePodB, Mockito.times(2))
                                        .convertAndSend(eq("/topic/notificacoes/" + usuarioId), captorB.capture()));

        assertThat(captorB.getAllValues()).extracting(NotificacaoPayload::seq).containsExactly(1L, 2L);
    }
}
