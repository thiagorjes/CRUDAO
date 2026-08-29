package com.crudao.kanban.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.crudao.kanban.domain.papel.UsuarioProjetoPapel;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

/**
 * Testes unitários para TASK-05.1 — BoardChannelInterceptor (autorização STOMP).
 *
 * Validam:
 * - Subscrição autorizada a /topic/board/{projetoId} com vínculo ao projeto
 * - Subscrição bloqueada sem vínculo
 * - Subscrição autorizada a /topic/notificacoes/{usuarioId} para o próprio usuário
 * - Subscrição bloqueada para outros usuários
 * - Usuários inativos bloqueados
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TASK-05.1: Autorização STOMP — BoardChannelInterceptor")
class BoardChannelInterceptorTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;

    private BoardChannelInterceptor interceptor;

    private UUID usuarioId;
    private UUID projetoId;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        interceptor = new BoardChannelInterceptor(
            usuarioProjetoPapelRepository,
            usuarioRepository
        );
        usuarioId = UUID.randomUUID();
        projetoId = UUID.randomUUID();

        usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setEmail("user@example.com");
        usuario.setNome("Test User");
        usuario.setAtivo(true);
    }

    @Test
    @DisplayName("Subscrição a /topic/board/{projetoId} AUTORIZADA com vínculo ao projeto (query consolidada)")
    void testSubscribeBoard_Autorizado() {
        // Arrange
        // Usa query consolidada em vez de 2 queries separadas (melhoria de performance TASK-05.1)
        when(usuarioProjetoPapelRepository.existeVinculoAtivoParaBoardProjeto("user@example.com", projetoId))
            .thenReturn(true);

        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("user@example.com");

        Message<?> message = criarSubscribeMessage(
            String.format("/topic/board/%s", projetoId),
            principal
        );

        // Act
        Message<?> result = interceptor.preSend(message, null);

        // Assert
        assertThat(result).isNotNull(); // Mensagem não foi bloqueada
    }

    @Test
    @DisplayName("Subscrição a /topic/board/{projetoId} BLOQUEADA sem vínculo ao projeto")
    void testSubscribeBoard_SemVinculo() {
        // Arrange
        when(usuarioProjetoPapelRepository.existeVinculoAtivoParaBoardProjeto("user@example.com", projetoId))
            .thenReturn(false);

        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("user@example.com");

        Message<?> message = criarSubscribeMessage(
            String.format("/topic/board/%s", projetoId),
            principal
        );

        // Act
        Message<?> result = interceptor.preSend(message, null);

        // Assert
        assertThat(result).isNull(); // Mensagem foi bloqueada
    }

    @Test
    @DisplayName("Subscrição a /topic/notificacoes/{usuarioId} AUTORIZADA para o próprio usuário")
    void testSubscribeNotificacoes_ProprioUsuario() {
        // Arrange
        when(usuarioRepository.findByEmail("user@example.com")).thenReturn(Optional.of(usuario));

        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("user@example.com");

        Message<?> message = criarSubscribeMessage(
            String.format("/topic/notificacoes/%s", usuarioId),
            principal
        );

        // Act
        Message<?> result = interceptor.preSend(message, null);

        // Assert
        assertThat(result).isNotNull(); // Mensagem não foi bloqueada
    }

    @Test
    @DisplayName("Subscrição a /topic/notificacoes/{usuarioId} BLOQUEADA para outro usuário")
    void testSubscribeNotificacoes_UsuarioDiferente() {
        // Arrange
        when(usuarioRepository.findByEmail("user@example.com")).thenReturn(Optional.of(usuario));

        UUID outroUsuarioId = UUID.randomUUID();

        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("user@example.com");

        Message<?> message = criarSubscribeMessage(
            String.format("/topic/notificacoes/%s", outroUsuarioId),
            principal
        );

        // Act
        Message<?> result = interceptor.preSend(message, null);

        // Assert
        assertThat(result).isNull(); // Mensagem foi bloqueada
    }

    @Test
    @DisplayName("Subscrição BLOQUEADA para usuário inativo")
    void testSubscribe_UsuarioInativo() {
        // Arrange
        usuario.setAtivo(false); // Usuário inativo
        when(usuarioRepository.findByEmail("user@example.com")).thenReturn(Optional.of(usuario));

        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("user@example.com");

        Message<?> message = criarSubscribeMessage(
            String.format("/topic/board/%s", projetoId),
            principal
        );

        // Act
        Message<?> result = interceptor.preSend(message, null);

        // Assert
        assertThat(result).isNull(); // Mensagem foi bloqueada
    }

    @Test
    @DisplayName("Mensagens não-SUBSCRIBE passam através do interceptor")
    void testNaoSubscribePassaThroughInterceptor() {
        // Arrange
        Message<?> message = MessageBuilder.withPayload("test").build();

        // Act
        Message<?> result = interceptor.preSend(message, null);

        // Assert
        assertThat(result).isSameAs(message); // Mensagem passou sem modificação
    }

    @Test
    @DisplayName("Subscrição com destination NULL é bloqueada")
    void testSubscribe_DestinationNull() {
        // Arrange
        // Nota: usuarioRepository não será chamado porque destination é null
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("user@example.com");

        Message<?> message = MessageBuilder
            .withPayload("test")
            .setHeader("stompCommand", StompCommand.SUBSCRIBE)
            .setHeader("simpUser", principal)
            // Intencionalmente sem setHeader("stompDestination", ...) para testar destination=null
            .build();

        // Act
        Message<?> result = interceptor.preSend(message, null);

        // Assert
        assertThat(result).isNull(); // Mensagem foi bloqueada
    }

    /**
     * Helper: cria uma Message STOMP SUBSCRIBE com destination e principal.
     */
    private Message<?> criarSubscribeMessage(String destination, Principal principal) {
        return MessageBuilder
            .withPayload("test")
            .setHeader("stompCommand", StompCommand.SUBSCRIBE)
            .setHeader("stompDestination", destination)
            .setHeader("simpUser", principal)
            .setHeader("simpDestination", destination)
            .build();
    }
}
