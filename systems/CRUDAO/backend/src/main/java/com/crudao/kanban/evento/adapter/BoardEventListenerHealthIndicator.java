package com.crudao.kanban.evento.adapter;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reflete o estado da conexão {@code LISTEN board_events} no readiness probe (ADR-004) — um pod
 * com listener desconectado não deve ser considerado saudável sob RNF-002 (perde eventos de board
 * em tempo real, mesmo que o board via GET continue funcionando).
 */
@Component("boardEventListenerHealthIndicator")
public class BoardEventListenerHealthIndicator implements HealthIndicator {

    private final BoardEventListener listener;

    public BoardEventListenerHealthIndicator(BoardEventListener listener) {
        this.listener = listener;
    }

    @Override
    public Health health() {
        return listener.isConectado() ? Health.up().build() : Health.down().build();
    }
}
