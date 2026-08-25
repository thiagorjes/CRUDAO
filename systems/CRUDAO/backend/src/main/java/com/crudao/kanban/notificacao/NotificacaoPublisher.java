package com.crudao.kanban.notificacao;

import com.crudao.kanban.domain.notificacao.Notificacao;

/**
 * Porta de domínio para publicação de eventos de notificação (RF-005) — desacoplada do mecanismo
 * de transporte (LISTEN/NOTIFY hoje), mesmo padrão de {@code EventoBoardPublisher} (ADR-004).
 * Implementada por {@code notificacao.adapter.ListenNotifyNotificacaoPublisher}.
 *
 * <p>Chamada pelo {@code NotificacaoService} dentro da mesma transação de escrita que criou a
 * {@link Notificacao} — a publicação real só ocorre após o commit ({@code
 * TransactionSynchronization.afterCommit}).
 */
public interface NotificacaoPublisher {

    void publicar(Notificacao notificacao);
}
