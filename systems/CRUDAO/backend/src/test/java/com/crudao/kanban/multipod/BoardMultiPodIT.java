package com.crudao.kanban.multipod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.crudao.kanban.evento.EventoBoardPayload;
import com.crudao.kanban.evento.TipoEventoBoard;
import com.crudao.kanban.evento.adapter.BoardEventListener;
import com.crudao.kanban.evento.adapter.ListenNotifyPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
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
 * Valida escalabilidade horizontal do canal {@code board_events} (RNF-001, RNF-002, ADR-004,
 * TASK-08.1) — duas instâncias de {@link BoardEventListener} ("pod A" e "pod B"), cada uma com seu
 * próprio {@link SimpMessagingTemplate} (simulando conexões STOMP distintas em pods diferentes),
 * compartilhando o mesmo Postgres. Um evento publicado (a publicação em si é indiferente a qual pod
 * a originou — {@code pg_notify} é broadcast) deve chegar a ambos os pods.
 *
 * <p><b>Sobre {@code seq}:</b> {@link ListenNotifyPublisher#sequenciasPorProjeto} é um contador em
 * memória por instância de publisher — cada pod de escrita tem sua própria sequência, não há
 * sequência global entre pods (documentado no Javadoc de {@link ListenNotifyPublisher}). Por isso a
 * resincronização client-side (ADR-004) não depende de um {@code seq} globalmente monotônico: os
 * testes aqui usam duas instâncias de publisher (uma por pod) para deixar esse comportamento
 * explícito — {@code seq} pode colidir entre pods, e mesmo assim o broadcast e o resync continuam
 * corretos.
 */
@Testcontainers
class BoardMultiPodIT {

    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private ObjectMapper objectMapper;
    private ListenNotifyPublisher publisher;
    private ListenNotifyPublisher publisherPodA;
    private ListenNotifyPublisher publisherPodB;
    private BoardEventListener listenerPodA;
    private BoardEventListener listenerPodB;
    private SimpMessagingTemplate templatePodA;
    private SimpMessagingTemplate templatePodB;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        publisher =
                new ListenNotifyPublisher(
                        objectMapper, POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        // Duas instâncias de publisher — cada uma com sua própria contagem de `seq` em memória,
        // simulando dois pods de escrita distintos gravando no mesmo Postgres.
        publisherPodA =
                new ListenNotifyPublisher(
                        objectMapper, POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        publisherPodB =
                new ListenNotifyPublisher(
                        objectMapper, POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());

        templatePodA = mock(SimpMessagingTemplate.class);
        listenerPodA =
                new BoardEventListener(
                        templatePodA,
                        objectMapper,
                        new SimpleMeterRegistry(),
                        POSTGRES.getJdbcUrl(),
                        POSTGRES.getUsername(),
                        POSTGRES.getPassword());

        templatePodB = mock(SimpMessagingTemplate.class);
        listenerPodB =
                new BoardEventListener(
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

    @Test
    void eventoPublicadoChegaAosClientesDosDoisPodsEmMenosDe2s() {
        UUID projetoId = UUID.randomUUID();
        UUID tarefaId = UUID.randomUUID();

        publisher.publicar(projetoId, TipoEventoBoard.TAREFA_MOVIDA, tarefaId);

        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () -> {
                            verify(templatePodA)
                                    .convertAndSend(eq("/topic/board/" + projetoId), any(EventoBoardPayload.class));
                            verify(templatePodB)
                                    .convertAndSend(eq("/topic/board/" + projetoId), any(EventoBoardPayload.class));
                        });
    }

    @Test
    void escritaOriginadaEmPodsDiferentes_ChegaAosClientesDosDoisPodsMesmoComSeqColidindoEntreSi() {
        // publisherPodA e publisherPodB são instâncias independentes (uma por pod de escrita) —
        // cada uma reinicia sua própria contagem de seq a partir de 1 para este projeto, então os
        // dois eventos abaixo chegam com seq=1 cada (colisão entre pods, esperada e documentada em
        // ListenNotifyPublisher). Mesmo assim, ambos os clientes STOMP (pod A e pod B) devem
        // receber os dois eventos — a resincronização client-side (ADR-004) não depende de seq
        // globalmente monotônico, só de detectar que algo mudou.
        UUID projetoId = UUID.randomUUID();

        publisherPodA.publicar(projetoId, TipoEventoBoard.TAREFA_CRIADA, UUID.randomUUID());
        publisherPodB.publicar(projetoId, TipoEventoBoard.TAREFA_MOVIDA, UUID.randomUUID());

        ArgumentCaptor<EventoBoardPayload> captorA = ArgumentCaptor.forClass(EventoBoardPayload.class);
        ArgumentCaptor<EventoBoardPayload> captorB = ArgumentCaptor.forClass(EventoBoardPayload.class);

        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () -> {
                            verify(templatePodA, Mockito.times(2))
                                    .convertAndSend(eq("/topic/board/" + projetoId), captorA.capture());
                            verify(templatePodB, Mockito.times(2))
                                    .convertAndSend(eq("/topic/board/" + projetoId), captorB.capture());
                        });

        assertThat(captorA.getAllValues()).extracting(EventoBoardPayload::seq).containsExactly(1L, 1L);
        assertThat(captorB.getAllValues()).extracting(EventoBoardPayload::seq).containsExactly(1L, 1L);
    }

    @Test
    void resincronizacaoPorGapDeSeq_PodDetectaEventoPerdidoAposReconexao() {
        // Um único pod de escrita (publisher) aqui — o gap de seq (1 -> 3) é local à contagem
        // desse publisher, o cenário real de client-side resync (ADR-004): o cliente conectado ao
        // pod A percebe que perdeu um evento e refaz o GET, independente do publisher ter trocado
        // de pod entre uma publicação e outra (ver teste anterior).
        UUID projetoId = UUID.randomUUID();

        // 1) pod A recebe o primeiro evento normalmente.
        publisher.publicar(projetoId, TipoEventoBoard.TAREFA_CRIADA, UUID.randomUUID());
        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                verify(templatePodA)
                                        .convertAndSend(eq("/topic/board/" + projetoId), any(EventoBoardPayload.class)));

        // 2) pod A cai (ex.: restart/rolling update) e perde o segundo evento; pod B continua no ar.
        listenerPodA.fecharConexaoAtualParaTeste();
        await().atMost(Duration.ofSeconds(2)).until(() -> !listenerPodA.isConectado());

        publisher.publicar(projetoId, TipoEventoBoard.TAREFA_MOVIDA, UUID.randomUUID());
        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                verify(templatePodB, Mockito.times(2))
                                        .convertAndSend(eq("/topic/board/" + projetoId), any(EventoBoardPayload.class)));

        // 3) pod A reconecta e volta a receber eventos — o cliente conectado a ele detecta o gap
        // de seq (1 -> 3, faltando o 2) e deve resincronizar via GET /board.
        await().atMost(Duration.ofSeconds(10)).until(listenerPodA::isConectado);
        publisher.publicar(projetoId, TipoEventoBoard.TAREFA_EXCLUIDA, UUID.randomUUID());

        ArgumentCaptor<EventoBoardPayload> captorA = ArgumentCaptor.forClass(EventoBoardPayload.class);
        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                verify(templatePodA, Mockito.times(2))
                                        .convertAndSend(eq("/topic/board/" + projetoId), captorA.capture()));

        var seqsRecebidosPeloPodA = captorA.getAllValues().stream().map(EventoBoardPayload::seq).sorted().toList();
        assertThat(seqsRecebidosPeloPodA).containsExactly(1L, 3L);
        assertThat(seqsRecebidosPeloPodA).doesNotContain(2L);
    }
}
