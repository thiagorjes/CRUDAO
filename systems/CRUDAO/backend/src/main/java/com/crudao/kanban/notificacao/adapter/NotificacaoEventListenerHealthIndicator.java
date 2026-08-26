package com.crudao.kanban.notificacao.adapter;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reflete o estado da conexão {@code LISTEN notificacao_events} no readiness probe — mesmo padrão
 * de {@code BoardEventListenerHealthIndicator} (ADR-004, TASK-05.1), estendido para o canal de
 * notificações em TASK-05.3: um pod com este listener desconectado não deve ser considerado
 * saudável (RF-005 perde notificações em tempo real, mesmo que o REST continue funcionando).
 */
@Component("notificacaoEventListenerHealthIndicator")
public class NotificacaoEventListenerHealthIndicator implements HealthIndicator {

    private final NotificacaoEventListener listener;

    public NotificacaoEventListenerHealthIndicator(NotificacaoEventListener listener) {
        this.listener = listener;
    }

    @Override
    public Health health() {
        return listener.isConectado() ? Health.up().build() : Health.down().build();
    }
}
