package com.crudao.kanban.security;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Exige um JWT válido (mesmo formato usado na API REST) no header STOMP {@code Authorization} do
 * frame CONNECT (RF-005, ADR-003) — achado de code review da TASK-05.1: o endpoint HTTP {@code
 * /ws/**} é {@code permitAll()} (necessário para o handshake de upgrade da conexão), mas isso não
 * deve significar que qualquer cliente WebSocket, sem autenticação nenhuma, possa se inscrever em
 * tópicos de board e observar dados de tarefas de qualquer projeto. A validação aqui acontece no
 * nível do frame STOMP, não do handshake HTTP.
 *
 * <p>Autorização por projeto (ex.: restringir a subscription a membros do projeto) não é aplicada
 * aqui — o RBAC atual do sistema é por papel/permissão global, sem escopo por projeto em nenhum
 * outro endpoint (ver RbacSeeder); o mínimo alcançável e consistente com o resto do sistema é
 * exigir um usuário autenticado.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

  private final JwtDecoder jwtDecoder;

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      String cabecalho = accessor.getFirstNativeHeader("Authorization");
      String token =
          cabecalho != null && cabecalho.startsWith("Bearer ") ? cabecalho.substring(7) : null;
      if (token == null) {
        throw new BadCredentialsException("Conexão STOMP sem token de autenticação.");
      }
      try {
        Jwt jwt = jwtDecoder.decode(token);
        accessor.setUser(new JwtPrincipalStomp(jwt.getSubject()));
      } catch (Exception e) {
        throw new BadCredentialsException("Token inválido na conexão STOMP.", e);
      }
    }
    return message;
  }
}
