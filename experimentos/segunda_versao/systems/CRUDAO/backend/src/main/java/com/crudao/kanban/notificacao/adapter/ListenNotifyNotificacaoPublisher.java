package com.crudao.kanban.notificacao.adapter;

import com.crudao.kanban.domain.notificacao.Notificacao;
import com.crudao.kanban.notificacao.NotificacaoPayload;
import com.crudao.kanban.notificacao.NotificacaoPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Adapter LISTEN/NOTIFY da porta {@link NotificacaoPublisher} (RF-005) — canal dedicado {@code
 * notificacao_events}, mesmo padrão de {@code ListenNotifyPublisher} (board, ADR-004, TASK-05.1):
 * conexão JDBC própria via {@link DriverManager} (fora do pool Hikari, ver Javadoc daquela classe
 * para o motivo), publicação só após o commit da transação.
 *
 * <p>Sequência ({@code seq}) incrementada por {@code usuarioId}, em memória por pod — o cliente
 * resincroniza via {@code GET /api/notificacoes} ao detectar gap ou reconexão.
 */
@Component
public class ListenNotifyNotificacaoPublisher implements NotificacaoPublisher {

    private static final Logger log = LoggerFactory.getLogger(ListenNotifyNotificacaoPublisher.class);
    private static final String CANAL = "notificacao_events";

    private final ObjectMapper objectMapper;
    private final String url;
    private final String username;
    private final String password;
    private final Map<UUID, AtomicLong> sequenciasPorUsuario = new ConcurrentHashMap<>();

    public ListenNotifyNotificacaoPublisher(
            ObjectMapper objectMapper,
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        this.objectMapper = objectMapper;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override
    public void publicar(Notificacao notificacao) {
        UUID usuarioId = notificacao.getUsuario().getId();
        long seq = sequenciasPorUsuario.computeIfAbsent(usuarioId, id -> new AtomicLong()).incrementAndGet();
        UUID id = notificacao.getId();
        UUID tarefaId = notificacao.getTarefa().getId();
        String tipo = notificacao.getTipo();
        String mensagem = notificacao.getMensagem();
        boolean lida = notificacao.isLida();
        var criadoEm = notificacao.getCriadoEm();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            notificar(id, usuarioId, tarefaId, tipo, mensagem, lida, criadoEm, seq);
                        }
                    });
        } else {
            notificar(id, usuarioId, tarefaId, tipo, mensagem, lida, criadoEm, seq);
        }
    }

    private void notificar(
            UUID id,
            UUID usuarioId,
            UUID tarefaId,
            String tipo,
            String mensagem,
            boolean lida,
            java.time.OffsetDateTime criadoEm,
            long seq) {
        // publicadoEmEpochMillis estampado aqui, no instante real do pg_notify — mesma decisão de
        // ListenNotifyPublisher (board, TASK-05.3): não usar criadoEm da entidade, que é anterior ao
        // commit e distorceria a latência NOTIFY→broadcast.
        NotificacaoPayload payload =
                new NotificacaoPayload(
                        id, usuarioId, tarefaId, tipo, mensagem, lida, criadoEm, seq, System.currentTimeMillis());
        try {
            String json = objectMapper.writeValueAsString(payload);
            try (Connection connection = DriverManager.getConnection(url, username, password);
                    PreparedStatement statement = connection.prepareStatement("SELECT pg_notify(?, ?)")) {
                statement.setString(1, CANAL);
                statement.setString(2, json);
                statement.execute();
            }
        } catch (Exception e) {
            // Best-effort — o cliente resincroniza via GET /api/notificacoes em caso de perda.
            log.error("Falha ao publicar evento de notificação (usuarioId={})", payload.usuarioId(), e);
        }
    }
}
