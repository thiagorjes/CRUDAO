package com.crudao.kanban.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Endpoint STOMP {@code /ws} (SockJS) — broadcast de board (ADR-004) e, futuramente, de
 * notificações por usuário. Handshake protegido pelo mesmo resource server opaco de {@code
 * /api/**} ({@link com.crudao.kanban.security.SecurityConfig}); autorização por tópico em {@link
 * BoardChannelInterceptor}.
 */
@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    private final AutenticacaoHandshakeInterceptor autenticacaoHandshakeInterceptor;
    private final BoardChannelInterceptor boardChannelInterceptor;

    public StompConfig(
            AutenticacaoHandshakeInterceptor autenticacaoHandshakeInterceptor,
            BoardChannelInterceptor boardChannelInterceptor) {
        this.autenticacaoHandshakeInterceptor = autenticacaoHandshakeInterceptor;
        this.boardChannelInterceptor = boardChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").addInterceptors(autenticacaoHandshakeInterceptor).withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(boardChannelInterceptor);
    }
}
