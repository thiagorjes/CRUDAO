package com.crudao.kanban.notificacao.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

/**
 * Readiness do pod deve refletir o estado da conexão {@code LISTEN notificacao_events} — mesmo
 * padrão de {@code BoardEventListenerHealthIndicatorTest} (TASK-05.1), estendido em TASK-05.3.
 */
class NotificacaoEventListenerHealthIndicatorTest {

    @Test
    void listenerConectado_healthUp() {
        NotificacaoEventListener listener = mock(NotificacaoEventListener.class);
        when(listener.isConectado()).thenReturn(true);

        var health = new NotificacaoEventListenerHealthIndicator(listener).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void listenerDesconectado_healthDown() {
        NotificacaoEventListener listener = mock(NotificacaoEventListener.class);
        when(listener.isConectado()).thenReturn(false);

        var health = new NotificacaoEventListenerHealthIndicator(listener).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}
