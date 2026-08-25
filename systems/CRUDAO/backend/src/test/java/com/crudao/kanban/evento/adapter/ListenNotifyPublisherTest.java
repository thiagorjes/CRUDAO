package com.crudao.kanban.evento.adapter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.evento.TipoEventoBoard;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Confirma que a publicação (RNF-001, ADR-004) só ocorre após o commit da transação de escrita
 * — não antes, para não notificar mudança sujeita a rollback tardio — e que {@code seq} incrementa
 * por {@code projetoId}. Não usa Testcontainers/Spring: {@link TransactionSynchronizationManager}
 * é gerenciado manualmente (sem subir uma transação real) e {@link DriverManager} é mockado
 * estaticamente — o adapter abre uma conexão própria por publicação (não usa o pool da
 * aplicação, achado de code review, agent QA, TASK-05.1), então não há um {@code DataSource} para
 * injetar.
 */
class ListenNotifyPublisherTest {

    private static final String URL = "jdbc:postgresql://localhost:5432/kanban";
    private static final String USERNAME = "kanban";
    private static final String PASSWORD = "kanban";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ListenNotifyPublisher publisher =
            new ListenNotifyPublisher(objectMapper, URL, USERNAME, PASSWORD);

    @AfterEach
    void limpar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void semTransacaoAtiva_notificaImediatamente() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement("SELECT pg_notify(?, ?)")).thenReturn(statement);

        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class)) {
            driverManager.when(() -> DriverManager.getConnection(URL, USERNAME, PASSWORD)).thenReturn(connection);

            publisher.publicar(UUID.randomUUID(), TipoEventoBoard.TAREFA_CRIADA, UUID.randomUUID());

            verify(statement).execute();
        }
    }

    @Test
    void comTransacaoAtiva_naoNotificaAntesDoCommit() {
        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class)) {
            TransactionSynchronizationManager.initSynchronization();
            try {
                publisher.publicar(UUID.randomUUID(), TipoEventoBoard.TAREFA_MOVIDA, UUID.randomUUID());
                driverManager.verify(() -> DriverManager.getConnection(URL, USERNAME, PASSWORD), never());
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }
    }

    @Test
    void comTransacaoAtiva_notificaSoAposAfterCommit() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement("SELECT pg_notify(?, ?)")).thenReturn(statement);

        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class)) {
            driverManager.when(() -> DriverManager.getConnection(URL, USERNAME, PASSWORD)).thenReturn(connection);

            TransactionSynchronizationManager.initSynchronization();
            publisher.publicar(UUID.randomUUID(), TipoEventoBoard.TAREFA_EXCLUIDA, UUID.randomUUID());
            verify(statement, never()).execute();

            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());

            verify(statement, times(1)).execute();
        }
    }

    @Test
    void seqIncrementaPorProjeto() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement("SELECT pg_notify(?, ?)")).thenReturn(statement);

        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class)) {
            driverManager.when(() -> DriverManager.getConnection(URL, USERNAME, PASSWORD)).thenReturn(connection);

            UUID projetoId = UUID.randomUUID();
            publisher.publicar(projetoId, TipoEventoBoard.TAREFA_CRIADA, UUID.randomUUID());
            publisher.publicar(projetoId, TipoEventoBoard.TAREFA_MOVIDA, UUID.randomUUID());

            var payloadCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(statement, times(2)).setString(eq(2), payloadCaptor.capture());
            String primeiro = payloadCaptor.getAllValues().get(0);
            String segundo = payloadCaptor.getAllValues().get(1);
            org.assertj.core.api.Assertions.assertThat(primeiro).contains("\"seq\":1");
            org.assertj.core.api.Assertions.assertThat(segundo).contains("\"seq\":2");
        }
    }
}
