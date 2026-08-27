package com.crudao.kanban.config;

import com.crudao.kanban.security.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Endpoint STOMP para atualizações em tempo real do board (RF-005, RNF-001). Broker simples em
 * memória: a distribuição entre pods é feita via LISTEN/NOTIFY do PostgreSQL (ADR-004), não pelo
 * broker STOMP em si — cada pod só precisa entregar aos clientes conectados localmente.
 *
 * <p>O handshake HTTP de upgrade (`/ws/**`) é {@code permitAll()} no {@code SecurityConfig} — a
 * autenticação de fato acontece no frame STOMP CONNECT, via {@link StompAuthChannelInterceptor}
 * (achado de code review da TASK-05.1).
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic");
    registry.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(stompAuthChannelInterceptor);
  }
}
