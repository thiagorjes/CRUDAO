package com.crudao.kanban.evento;

import java.util.UUID;

/**
 * Porta de domínio para publicação de eventos de notificação.
 * Desacopla a lógica de negócio do mecanismo de transporte (LISTEN/NOTIFY, STOMP, etc).
 * Implementação via adapter: {@link com.crudao.kanban.evento.adapter.ListenNotifyNotificacaoPublisher}.
 *
 * ADR-004: Broadcast multi-pod via PostgreSQL LISTEN/NOTIFY.
 * TASK-05.2: Notificações internas por usuário.
 */
public interface NotificacaoEventPublisher {

    /**
     * Publica um evento de notificação para entrega em tempo real.
     *
     * @param evento Envelope contendo tipo, usuarioId, tarefaId e payload do evento.
     */
    void publicar(NotificacaoEventPayload evento);

    /**
     * Payload de um evento de notificação.
     *
     * @param tipo Tipo de notificação (TRANSICAO_ETAPA, IMPEDIMENTO_MARCADO, IMPEDIMENTO_DESMARCADO).
     * @param usuarioId ID do usuário observador que receberá a notificação.
     * @param tarefaId ID da tarefa que alterou.
     * @param seq Sequência incremental para detecção de gap na subscrição (ADR-004).
     * @param payload JSON contendo detalhes específicos (ex: {"tarefaId": "...", "tipo": "..."}).
     */
    record NotificacaoEventPayload(
            String tipo,
            UUID usuarioId,
            UUID tarefaId,
            long seq,
            String payload
    ) {}
}
