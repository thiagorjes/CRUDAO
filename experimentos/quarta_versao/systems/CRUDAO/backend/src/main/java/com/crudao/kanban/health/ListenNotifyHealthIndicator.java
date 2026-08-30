package com.crudao.kanban.health;

import com.crudao.kanban.evento.adapter.AbstractListenNotifyRelay;
import java.util.List;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Readiness do transporte LISTEN/NOTIFY (TASK-05.3): {@code /actuator/health/listenNotify}
 * reporta {@code DOWN} enquanto qualquer listener (board ou notificações) estiver desconectado.
 *
 * <p>Registrado no grupo {@code readiness} — durante a reconexão do listener o pod deixa de
 * receber tráfego até restabelecer o {@code LISTEN} (ADR-004, RNF-002).
 */
@Component("listenNotify")
public class ListenNotifyHealthIndicator implements HealthIndicator {

    private final List<AbstractListenNotifyRelay<?>> relays;

    public ListenNotifyHealthIndicator(List<AbstractListenNotifyRelay<?>> relays) {
        this.relays = relays;
    }

    @Override
    public Health health() {
        Health.Builder builder = relays.stream().allMatch(AbstractListenNotifyRelay::isConectado)
                ? Health.up()
                : Health.down();
        for (AbstractListenNotifyRelay<?> relay : relays) {
            builder.withDetail(relay.nomeCanal(), relay.isConectado() ? "conectado" : "desconectado");
        }
        return builder.build();
    }
}
