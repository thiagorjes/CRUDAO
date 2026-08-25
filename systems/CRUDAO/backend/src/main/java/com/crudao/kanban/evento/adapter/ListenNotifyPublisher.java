package com.crudao.kanban.evento.adapter;

import com.crudao.kanban.evento.EventoBoardPayload;
import com.crudao.kanban.evento.EventoBoardPublisher;
import com.crudao.kanban.evento.TipoEventoBoard;
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
 * Adapter LISTEN/NOTIFY da porta {@link EventoBoardPublisher} (ADR-004). Publica via {@code
 * pg_notify(channel, payload)} — evita concatenar o JSON diretamente no SQL (o payload pode conter
 * aspas).
 *
 * <p>Usa uma conexão JDBC própria (fora do pool Hikari, via {@link DriverManager}), aberta e
 * fechada a cada publicação — não disputa capacidade com o pool de conexões transacionais da
 * aplicação. Isso importa em especial porque a publicação roda dentro de {@code afterCommit}
 * (achado de code review, agent QA, TASK-05.1): nesse ponto a conexão da transação que acabou de
 * commitar ainda não foi devolvida ao pool (Spring libera em {@code afterCompletion}, que roda
 * depois), então pegar uma conexão do mesmo pool aqui poderia esgotá-lo sob carga. A conexão
 * dedicada em {@code LISTEN} fica em {@link BoardEventListener}.
 *
 * <p>Sequência ({@code seq}) incrementada por {@code projetoId}, em memória por pod — cada pod tem
 * sua própria contagem; o cliente resincroniza via {@code GET} ao detectar gap ou reconexão
 * (resincronização client-side, ADR-004), não depende de sequência global.
 */
@Component
public class ListenNotifyPublisher implements EventoBoardPublisher {

    private static final Logger log = LoggerFactory.getLogger(ListenNotifyPublisher.class);
    private static final String CANAL = "board_events";

    private final ObjectMapper objectMapper;
    private final String url;
    private final String username;
    private final String password;
    private final Map<UUID, AtomicLong> sequenciasPorProjeto = new ConcurrentHashMap<>();

    public ListenNotifyPublisher(
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
    public void publicar(UUID projetoId, TipoEventoBoard tipo, UUID tarefaId) {
        long seq = sequenciasPorProjeto.computeIfAbsent(projetoId, id -> new AtomicLong()).incrementAndGet();
        EventoBoardPayload payload = new EventoBoardPayload(tipo, projetoId, tarefaId, seq);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            notificar(payload);
                        }
                    });
        } else {
            notificar(payload);
        }
    }

    private void notificar(EventoBoardPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            try (Connection connection = DriverManager.getConnection(url, username, password);
                    PreparedStatement statement = connection.prepareStatement("SELECT pg_notify(?, ?)")) {
                statement.setString(1, CANAL);
                statement.setString(2, json);
                statement.execute();
            }
        } catch (Exception e) {
            // Falha de publicação não deve propagar para o fluxo de negócio já commitado — o
            // evento é best-effort (ADR-004); o cliente resincroniza via GET em caso de perda.
            log.error("Falha ao publicar evento de board (projetoId={}, tipo={})", payload.projetoId(), payload.tipo(), e);
        }
    }
}
