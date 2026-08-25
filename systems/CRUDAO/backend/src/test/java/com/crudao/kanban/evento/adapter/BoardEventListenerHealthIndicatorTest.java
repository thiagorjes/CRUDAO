package com.crudao.kanban.evento.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

/**
 * Readiness do pod deve refletir o estado da conexão {@code LISTEN board_events} (ADR-004,
 * consequência "um pod com listener desconectado não deve ser considerado saudável sob RNF-002") —
 * achado de code review (agent QA, TASK-05.1): sem este teste, uma regressão no cabeamento entre
 * {@link BoardEventListener#isConectado()} e o {@code HealthIndicator} passaria despercebida.
 */
class BoardEventListenerHealthIndicatorTest {

    @Test
    void listenerConectado_healthUp() {
        BoardEventListener listener = mock(BoardEventListener.class);
        when(listener.isConectado()).thenReturn(true);

        var health = new BoardEventListenerHealthIndicator(listener).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void listenerDesconectado_healthDown() {
        BoardEventListener listener = mock(BoardEventListener.class);
        when(listener.isConectado()).thenReturn(false);

        var health = new BoardEventListenerHealthIndicator(listener).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}
