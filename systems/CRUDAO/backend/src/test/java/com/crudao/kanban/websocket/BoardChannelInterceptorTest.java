package com.crudao.kanban.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Autorização de subscrição STOMP em {@code /topic/board/{projetoId}} (Seção 5 da TechSpec,
 * RF-013/RNF-003, TASK-05.1) — subscrição sem vínculo ao projeto deve ser rejeitada.
 */
@ExtendWith(MockitoExtension.class)
class BoardChannelInterceptorTest {

    @Mock private PermissaoGuard permissaoGuard;
    @Mock private UsuarioRepository usuarioRepository;

    private BoardChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new BoardChannelInterceptor(permissaoGuard, usuarioRepository);
    }

    @AfterEach
    void limpar() {
        UsuarioAutenticadoHolder.clear();
    }

    @Test
    void subscricaoSemVinculoAoProjeto_lancaExcecaoQueViraErrorStomp() {
        UUID usuarioId = UUID.randomUUID();
        UUID projetoId = UUID.randomUUID();
        Usuario usuario = usuario(usuarioId);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(permissaoGuard.membro(projetoId)).thenReturn(false);

        Message<byte[]> subscribe = mensagemSubscribe("/topic/board/" + projetoId, usuarioId);

        assertThatThrownBy(() -> interceptor.preSend(subscribe, null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void subscricaoComVinculoAoProjeto_permitida() {
        UUID usuarioId = UUID.randomUUID();
        UUID projetoId = UUID.randomUUID();
        Usuario usuario = usuario(usuarioId);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);

        Message<byte[]> subscribe = mensagemSubscribe("/topic/board/" + projetoId, usuarioId);

        assertThat(interceptor.preSend(subscribe, null)).isEqualTo(subscribe);
    }

    @Test
    void subscricaoSemUsuarioNaSessao_rejeitada() {
        UUID projetoId = UUID.randomUUID();
        Message<byte[]> subscribe = mensagemSubscribe("/topic/board/" + projetoId, null);

        assertThatThrownBy(() -> interceptor.preSend(subscribe, null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void subscricaoAOutroTopico_naoInterceptada() {
        Message<byte[]> subscribe = mensagemSubscribe("/topic/notificacoes/" + UUID.randomUUID(), UUID.randomUUID());

        assertThat(interceptor.preSend(subscribe, null)).isEqualTo(subscribe);
    }

    private Message<byte[]> mensagemSubscribe(String destino, UUID usuarioId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destino);
        accessor.setSessionAttributes(
                usuarioId == null
                        ? Map.of()
                        : Map.of(AutenticacaoHandshakeInterceptor.ATRIBUTO_USUARIO_ID, usuarioId));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Usuario usuario(UUID id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setKeycloakSub("sub-" + id);
        usuario.setNome("Usuário Teste");
        usuario.setEmail(id + "@teste.com");
        usuario.setAtivo(true);
        usuario.setCriadoEm(OffsetDateTime.now());
        return usuario;
    }
}
