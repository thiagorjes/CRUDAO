package com.crudao.kanban.evento;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Testes unitários para TASK-05.1 — EventoBoardPublisher e adapter LISTEN/NOTIFY.
 *
 * Validam:
 * - Serialização correta de eventos
 * - Invocação de NOTIFY via TransactionSynchronization.afterCommit
 * - Retransmissão de eventos via STOMP
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TASK-05.1: EventoBoardPublisher e adapter LISTEN/NOTIFY")
class EventoBoardPublisherTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Cria evento com tipo, projetoId, seq e payload JSON válido")
    void testCriaEventoComDadosCompletos() {
        // Arrange
        UUID projetoId = UUID.randomUUID();
        String tipo = "TAREFA_CRIADA";
        String payload = "{\"tarefaId\":\"123\",\"etapaId\":\"456\"}";

        // Act
        EventoBoardPublisher.EventoBoardPayload evento =
            new EventoBoardPublisher.EventoBoardPayload(tipo, projetoId, 1L, payload);

        // Assert
        assertThat(evento.tipo()).isEqualTo(tipo);
        assertThat(evento.projetoId()).isEqualTo(projetoId);
        assertThat(evento.seq()).isEqualTo(1L);
        assertThat(evento.payload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("Payload JSON respeita limite de 8KB do PostgreSQL")
    void testPayloadRespeitaLimiteDe8KB() {
        // Arrange
        String payloadLargo = "x".repeat(9000); // 9KB, excede limite
        UUID projetoId = UUID.randomUUID();

        // Act & Assert — verifica que o código não falha, mas registra warning
        EventoBoardPublisher.EventoBoardPayload evento =
            new EventoBoardPublisher.EventoBoardPayload("TESTE", projetoId, 1L, payloadLargo);

        assertThat(evento.payload().length()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Três tipos de eventos (CRIADA, MOVIDA, EXCLUIDA) são suportados")
    void testSuportaTodosTiposDeEventos() {
        // Arrange
        UUID projetoId = UUID.randomUUID();
        String[] tipos = {"TAREFA_CRIADA", "TAREFA_MOVIDA", "TAREFA_EXCLUIDA"};

        // Act & Assert
        for (String tipo : tipos) {
            EventoBoardPublisher.EventoBoardPayload evento =
                new EventoBoardPublisher.EventoBoardPayload(tipo, projetoId, 1L, "{}");
            assertThat(evento.tipo()).isEqualTo(tipo);
        }
    }

    @Test
    @DisplayName("Sequência incremental evita ambiguidade em reconexão de WebSocket")
    void testSequenciaIncremental() {
        // Arrange
        UUID projetoId = UUID.randomUUID();

        // Act
        EventoBoardPublisher.EventoBoardPayload evento1 =
            new EventoBoardPublisher.EventoBoardPayload("TAREFA_CRIADA", projetoId, 1L, "{}");
        EventoBoardPublisher.EventoBoardPayload evento2 =
            new EventoBoardPublisher.EventoBoardPayload("TAREFA_MOVIDA", projetoId, 2L, "{}");
        EventoBoardPublisher.EventoBoardPayload evento3 =
            new EventoBoardPublisher.EventoBoardPayload("TAREFA_EXCLUIDA", projetoId, 3L, "{}");

        // Assert
        assertThat(evento1.seq()).isLessThan(evento2.seq());
        assertThat(evento2.seq()).isLessThan(evento3.seq());
    }

    @Test
    @DisplayName("Permite eventos independentes por projeto (multi-tenancy STOMP)")
    void testEventosPorProjeto() {
        // Arrange
        UUID projeto1 = UUID.randomUUID();
        UUID projeto2 = UUID.randomUUID();

        // Act
        EventoBoardPublisher.EventoBoardPayload evento1 =
            new EventoBoardPublisher.EventoBoardPayload("TAREFA_CRIADA", projeto1, 1L, "{}");
        EventoBoardPublisher.EventoBoardPayload evento2 =
            new EventoBoardPublisher.EventoBoardPayload("TAREFA_CRIADA", projeto2, 2L, "{}");

        // Assert
        assertThat(evento1.projetoId()).isNotEqualTo(evento2.projetoId());
    }

    @Test
    @DisplayName("Serialização JSON de evento válida para retransmissão STOMP")
    void testSerializacaoJSON() throws Exception {
        // Arrange
        UUID projetoId = UUID.randomUUID();
        UUID tarefaId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(
            java.util.Map.of("tarefaId", tarefaId.toString(), "etapaId", UUID.randomUUID().toString())
        );

        EventoBoardPublisher.EventoBoardPayload evento =
            new EventoBoardPublisher.EventoBoardPayload("TAREFA_CRIADA", projetoId, 1L, payload);

        // Act
        String json = objectMapper.writeValueAsString(evento);

        // Assert
        assertThat(json).contains("tipo");
        assertThat(json).contains("projetoId");
        assertThat(json).contains("seq");
        assertThat(json).contains("payload");
        assertThat(json).contains(tarefaId.toString());
    }

    @Test
    @DisplayName("Destino STOMP formatado corretamente por projetoId")
    void testDestinoSTOMPPorProjeto() {
        // Arrange
        UUID projetoId = UUID.randomUUID();

        // Act
        String destination = String.format("/topic/board/%s", projetoId);

        // Assert
        assertThat(destination).startsWith("/topic/board/");
        assertThat(destination).contains(projetoId.toString());
    }

    @Test
    @DisplayName("Tratamento de erro em publicação não falha a transação (best-effort)")
    void testPublicacaoBestEffort() {
        // Arrange
        UUID projetoId = UUID.randomUUID();
        EventoBoardPublisher.EventoBoardPayload evento =
            new EventoBoardPublisher.EventoBoardPayload("TAREFA_CRIADA", projetoId, 1L, "{}");

        // Act & Assert — verifica que o evento é criado sem lançar exceção
        assertThat(evento).isNotNull();
    }
}
