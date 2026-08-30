package com.crudao.kanban.evento.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TASK-05.3 — backoff exponencial com teto da reconexão do listener LISTEN/NOTIFY.
 */
@DisplayName("TASK-05.3: backoff de reconexão do listener")
class AbstractListenNotifyRelayBackoffTest {

    @Test
    @DisplayName("Primeira tentativa espera o intervalo base (1s)")
    void primeiraTentativaUsaIntervaloBase() {
        assertThat(AbstractListenNotifyRelay.backoffDelayMs(1)).isEqualTo(1_000L);
    }

    @Test
    @DisplayName("Cresce exponencialmente: 1s, 2s, 4s, 8s, 16s")
    void cresceExponencialmente() {
        assertThat(AbstractListenNotifyRelay.backoffDelayMs(2)).isEqualTo(2_000L);
        assertThat(AbstractListenNotifyRelay.backoffDelayMs(3)).isEqualTo(4_000L);
        assertThat(AbstractListenNotifyRelay.backoffDelayMs(4)).isEqualTo(8_000L);
        assertThat(AbstractListenNotifyRelay.backoffDelayMs(5)).isEqualTo(16_000L);
    }

    @Test
    @DisplayName("Satura no teto de 30s e permanece lá (reconexão infinita)")
    void saturaNoTeto() {
        assertThat(AbstractListenNotifyRelay.backoffDelayMs(6)).isEqualTo(30_000L);
        assertThat(AbstractListenNotifyRelay.backoffDelayMs(10)).isEqualTo(30_000L);
        assertThat(AbstractListenNotifyRelay.backoffDelayMs(1_000)).isEqualTo(30_000L);
    }

    @Test
    @DisplayName("Tentativa não positiva não gera espera")
    void tentativaNaoPositiva() {
        assertThat(AbstractListenNotifyRelay.backoffDelayMs(0)).isZero();
        assertThat(AbstractListenNotifyRelay.backoffDelayMs(-3)).isZero();
    }
}
