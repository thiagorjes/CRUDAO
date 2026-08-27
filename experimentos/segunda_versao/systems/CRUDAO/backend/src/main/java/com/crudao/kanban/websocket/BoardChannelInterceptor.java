package com.crudao.kanban.websocket;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Autoriza subscrições STOMP a {@code /topic/board/{projetoId}} (Seção 5 da TechSpec, RNF-003
 * também no canal WebSocket): exige vínculo {@code UsuarioProjetoPapel} ativo com o projeto, mesmo
 * critério de {@code PermissaoGuard.membro} usado pelos GETs REST do board. Subscrição sem vínculo
 * é rejeitada — lançar aqui faz o {@code StompSubProtocolHandler} responder com um frame {@code
 * ERROR} ao cliente.
 */
@Component
public class BoardChannelInterceptor implements ChannelInterceptor {

    private static final Pattern TOPICO_BOARD =
            Pattern.compile("^/topic/board/([0-9a-fA-F-]{36})$");

    private final PermissaoGuard permissaoGuard;
    private final UsuarioRepository usuarioRepository;

    public BoardChannelInterceptor(PermissaoGuard permissaoGuard, UsuarioRepository usuarioRepository) {
        this.permissaoGuard = permissaoGuard;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destino = accessor.getDestination();
        Matcher matcher = TOPICO_BOARD.matcher(destino == null ? "" : destino);
        if (!matcher.matches()) {
            return message;
        }

        UUID projetoId = UUID.fromString(matcher.group(1));
        Object usuarioIdAtributo =
                accessor.getSessionAttributes() != null
                        ? accessor.getSessionAttributes().get(AutenticacaoHandshakeInterceptor.ATRIBUTO_USUARIO_ID)
                        : null;

        if (!(usuarioIdAtributo instanceof UUID usuarioId) || !autorizado(usuarioId, projetoId)) {
            throw new MessageDeliveryException("subscrição não autorizada");
        }

        return message;
    }

    private boolean autorizado(UUID usuarioId, UUID projetoId) {
        return usuarioRepository
                .findById(usuarioId)
                .map(
                        usuario -> {
                            Usuario anterior = UsuarioAutenticadoHolder.get();
                            UsuarioAutenticadoHolder.set(usuario);
                            try {
                                return permissaoGuard.membro(projetoId);
                            } finally {
                                if (anterior != null) {
                                    UsuarioAutenticadoHolder.set(anterior);
                                } else {
                                    UsuarioAutenticadoHolder.clear();
                                }
                            }
                        })
                .orElse(false);
    }
}
