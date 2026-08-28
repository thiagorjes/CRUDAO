package com.crudao.kanban.evento;

import java.util.UUID;

/**
 * Porta de domínio para publicação de eventos de board.
 * Desacopla a lógica de negócio do mecanismo de transporte (LISTEN/NOTIFY, STOMP, etc).
 * Implementação via adapter: {@link com.crudao.kanban.evento.adapter.ListenNotifyPublisher}.
 *
 * ADR-004: Broadcast multi-pod via PostgreSQL LISTEN/NOTIFY.
 */
public interface EventoBoardPublisher {

    /**
     * Publica um evento de board para atualização em tempo real.
     *
     * @param evento Envelope contendo tipo, projetoId e payload do evento.
     */
    void publicar(EventoBoardPayload evento);

    /**
     * Payload de um evento de board.
     *
     * @param tipo Tipo do evento (ex: TAREFA_CRIADA, TAREFA_MOVIDA, TAREFA_EXCLUIDA).
     * @param projetoId ID do projeto afetado.
     * @param seq Sequência incremental para detecção de gap na subscrição (ADR-004).
     * @param payload JSON contendo detalhes específicos do evento (ex: {"tarefaId": "...", "etapaId": "..."}).
     */
    record EventoBoardPayload(
            String tipo,
            UUID projetoId,
            long seq,
            String payload
    ) {}
}
