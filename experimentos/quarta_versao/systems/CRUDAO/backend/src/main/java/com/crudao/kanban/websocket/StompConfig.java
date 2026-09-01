package com.crudao.kanban.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuração de WebSocket com STOMP para suportar atualização em tempo real do board.
 *
 * Fluxo:
 * 1. Cliente conecta a `/ws` via WebSocket puro, autenticado por ticket de curta duração
 *    (`?ticket=`, validado por {@link WsTicketAuthenticationFilter} — TASK-07.7). Sem SockJS.
 * 2. Cliente subscreve a `/topic/board/{projetoId}` ou `/topic/notificacoes/{usuarioId}`.
 * 3. {@link BoardChannelInterceptor} valida autorização no handshake SUBSCRIBE.
 * 4. Adapter LISTEN/NOTIFY publica eventos para o tópico correspondente.
 * 5. Broker distribui mensagens aos clientes subscritos.
 *
 * RNF-001: Latência de broadcast <2s (validado com Awaitility em testes).
 * RNF-003: Autorização validada no backend (subscrição sem acesso → ERROR STOMP).
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    private final BoardChannelInterceptor boardChannelInterceptor;

    /**
     * Configura o broker de mensagens de aplicação (SimpleBrokerMessageHandler).
     * Em produção, considerar usar um broker dedicado (RabbitMQ, ActiveMQ) para escalabilidade multi-pod.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config
            .enableSimpleBroker("/topic", "/queue")
            .setHeartbeatValue(new long[]{25000, 25000}) // Heartbeat a cada 25s
            // O SimpleBroker exige um TaskScheduler quando o heartbeat está ligado
            // (senão o bean 'simpleBrokerMessageHandler' falha ao iniciar).
            .setTaskScheduler(webSocketHeartbeatScheduler());
    }

    /** Scheduler dedicado para os heartbeats do SimpleBroker STOMP. */
    @Bean
    public TaskScheduler webSocketHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("wss-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Registra o endpoint STOMP para conexão WebSocket.
     * Cliente faz `stompClient.connect('ws://localhost:8081/ws', headers, callback)`.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket puro — os clientes STOMP do frontend (board e notificações) conectam
        // com `new WebSocket('ws://.../ws?ticket=...')`. A autenticação do handshake vem do
        // WsTicketAuthenticationFilter (TASK-07.7). SockJS foi removido: nenhum cliente o usa e o
        // `new WebSocket()` cru é incompatível com o protocolo SockJS.
        registry.addEndpoint("/ws")
            .setAllowedOrigins("*"); // Em produção, restringir a origins conhecidos
    }

    /**
     * Registra interceptors para validar autorização na subscrição (SUBSCRIBE).
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(boardChannelInterceptor);
    }
}
