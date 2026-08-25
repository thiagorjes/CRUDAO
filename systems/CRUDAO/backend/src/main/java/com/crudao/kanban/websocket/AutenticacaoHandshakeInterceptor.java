package com.crudao.kanban.websocket;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.usuario.Usuario;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Captura o {@link Usuario} já resolvido pelo {@code AtivoUsuarioFilter} durante o handshake HTTP
 * (rota {@code /ws} protegida pelo mesmo resource server opaco de {@code /api/**}) e grava seu id
 * nos atributos da sessão WebSocket — o {@code ThreadLocal} de {@link UsuarioAutenticadoHolder} não
 * sobrevive além do handshake, então precisa ser propagado explicitamente para uso posterior em
 * {@link BoardChannelInterceptor} (autorização de subscrição).
 */
@Component
public class AutenticacaoHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATRIBUTO_USUARIO_ID = "usuarioId";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        Usuario usuario = UsuarioAutenticadoHolder.get();
        if (usuario == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(ATRIBUTO_USUARIO_ID, usuario.getId());
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // nada a fazer
    }
}
