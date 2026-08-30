package com.crudao.kanban.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.crudao.kanban.evento.adapter.AbstractListenNotifyRelay;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

/**
 * TASK-05.3 — readiness probe do transporte LISTEN/NOTIFY.
 */
@DisplayName("TASK-05.3: ListenNotifyHealthIndicator")
class ListenNotifyHealthIndicatorTest {

    private AbstractListenNotifyRelay<?> relay(String canal, boolean conectado) {
        AbstractListenNotifyRelay<?> r = mock(AbstractListenNotifyRelay.class);
        when(r.nomeCanal()).thenReturn(canal);
        when(r.isConectado()).thenReturn(conectado);
        return r;
    }

    @Test
    @DisplayName("UP quando todos os listeners estão conectados")
    void upQuandoTodosConectados() {
        var indicator = new ListenNotifyHealthIndicator(
                List.of(relay("board_events", true), relay("notificacao_events", true)));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("board_events", "conectado");
    }

    @Test
    @DisplayName("DOWN quando qualquer listener está desconectado, com detalhe por canal")
    void downQuandoAlgumDesconectado() {
        var indicator = new ListenNotifyHealthIndicator(
                List.of(relay("board_events", true), relay("notificacao_events", false)));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("board_events", "conectado")
                .containsEntry("notificacao_events", "desconectado");
    }
}
