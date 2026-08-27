package com.crudao.kanban.websocket;

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
 * Autoriza subscrições STOMP a {@code /topic/notificacoes/{usuarioId}} (RF-005,
 * `dashboard-notificacoes.md`, TASK-05.2): só é aceita se {@code usuarioId} do tópico == usuário
 * autenticado da sessão WebSocket (não vínculo de projeto, como em {@link
 * BoardChannelInterceptor} — aqui a checagem é "é você mesmo"). Subscrição de terceiros é
 * rejeitada com {@code ERROR} STOMP.
 */
@Component
public class NotificacaoChannelInterceptor implements ChannelInterceptor {

    private static final Pattern TOPICO_NOTIFICACOES =
            Pattern.compile("^/topic/notificacoes/([0-9a-fA-F-]{36})$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destino = accessor.getDestination();
        Matcher matcher = TOPICO_NOTIFICACOES.matcher(destino == null ? "" : destino);
        if (!matcher.matches()) {
            return message;
        }

        UUID usuarioIdDoTopico = UUID.fromString(matcher.group(1));
        Object usuarioIdAtributo =
                accessor.getSessionAttributes() != null
                        ? accessor.getSessionAttributes().get(AutenticacaoHandshakeInterceptor.ATRIBUTO_USUARIO_ID)
                        : null;

        if (!(usuarioIdAtributo instanceof UUID usuarioId) || !usuarioId.equals(usuarioIdDoTopico)) {
            throw new MessageDeliveryException("subscrição não autorizada");
        }

        return message;
    }
}
