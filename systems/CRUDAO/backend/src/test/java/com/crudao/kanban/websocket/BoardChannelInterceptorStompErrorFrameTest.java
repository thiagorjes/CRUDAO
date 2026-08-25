package com.crudao.kanban.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;

/**
 * Confirma, fim a fim através de {@link StompSubProtocolHandler} (não só o interceptor isolado),
 * que a rejeição de subscrição sem vínculo ao projeto realmente vira um frame STOMP {@code ERROR}
 * enviado ao cliente — não apenas uma exceção Java interna (achado de code review, agent QA,
 * TASK-05.1: a inferência de que {@code MessageDeliveryException} vira {@code ERROR} dependia do
 * comportamento do framework, não estava coberta por teste).
 */
class BoardChannelInterceptorStompErrorFrameTest {

    @Test
    void subscricaoSemVinculo_resultaEmFrameErrorEnviadoAoCliente() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        UUID projetoId = UUID.randomUUID();

        PermissaoGuard permissaoGuard = mock(PermissaoGuard.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario(usuarioId)));
        when(permissaoGuard.membro(projetoId)).thenReturn(false);

        BoardChannelInterceptor interceptor = new BoardChannelInterceptor(permissaoGuard, usuarioRepository);
        ExecutorSubscribableChannel canalEntrada = new ExecutorSubscribableChannel();
        canalEntrada.addInterceptor(interceptor);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("sessao-teste");
        when(session.getAttributes())
                .thenReturn(
                        new ConcurrentHashMap<>(
                                Map.of(AutenticacaoHandshakeInterceptor.ATRIBUTO_USUARIO_ID, usuarioId)));
        when(session.isOpen()).thenReturn(true);

        StompSubProtocolHandler handler = new StompSubProtocolHandler();
        handler.afterSessionStarted(session, canalEntrada);
        handler.handleMessageFromClient(session, new TextMessage(frameConnect()), canalEntrada);
        handler.handleMessageFromClient(
                session, new TextMessage(frameSubscribe(projetoId)), canalEntrada);

        ArgumentCaptor<WebSocketMessage<?>> captor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(session, org.mockito.Mockito.atLeastOnce()).sendMessage(captor.capture());

        boolean recebeuFrameError =
                captor.getAllValues().stream()
                        .anyMatch(mensagem -> String.valueOf(mensagem.getPayload()).startsWith("ERROR"));
        assertThat(recebeuFrameError).isTrue();
    }

    private String frameConnect() {
        return "CONNECT\naccept-version:1.2\nhost:localhost\n\n\0";
    }

    private String frameSubscribe(UUID projetoId) {
        return "SUBSCRIBE\nid:0\ndestination:/topic/board/" + projetoId + "\n\n\0";
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
