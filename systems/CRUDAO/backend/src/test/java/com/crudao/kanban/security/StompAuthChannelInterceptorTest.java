package com.crudao.kanban.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Achado de code review da TASK-05.1: sem esta validação, qualquer cliente WebSocket sem
 * autenticação podia se inscrever no tópico de board e observar dados de tarefas de qualquer
 * projeto.
 */
@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

  @Mock private JwtDecoder jwtDecoder;
  @Mock private MessageChannel channel;

  private StompAuthChannelInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor = new StompAuthChannelInterceptor(jwtDecoder);
  }

  @Test
  void deveRejeitarConnectSemHeaderAuthorization() {
    Message<byte[]> mensagem = connectSemAuthorization();

    assertThatThrownBy(() -> interceptor.preSend(mensagem, channel))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void deveRejeitarConnectComTokenInvalido() {
    when(jwtDecoder.decode("token-invalido")).thenThrow(new RuntimeException("assinatura inválida"));
    Message<byte[]> mensagem = connectCom("Bearer token-invalido");

    assertThatThrownBy(() -> interceptor.preSend(mensagem, channel))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void devePermitirConnectComTokenValido() {
    Jwt jwt = mock(Jwt.class);
    when(jwt.getSubject()).thenReturn("usuario-1");
    when(jwtDecoder.decode("token-valido")).thenReturn(jwt);
    Message<byte[]> mensagem = connectCom("Bearer token-valido");

    Message<?> resultado = interceptor.preSend(mensagem, channel);

    assertThat(resultado).isNotNull();
  }

  @Test
  void naoInterfereEmFramesQueNaoSaoConnect() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
    Message<byte[]> mensagem = org.springframework.messaging.support.MessageBuilder
        .createMessage(new byte[0], accessor.getMessageHeaders());

    Message<?> resultado = interceptor.preSend(mensagem, channel);

    assertThat(resultado).isSameAs(mensagem);
  }

  private Message<byte[]> connectSemAuthorization() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    return org.springframework.messaging.support.MessageBuilder.createMessage(
        new byte[0], accessor.getMessageHeaders());
  }

  private Message<byte[]> connectCom(String headerAuthorization) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setNativeHeader("Authorization", headerAuthorization);
    return org.springframework.messaging.support.MessageBuilder.createMessage(
        new byte[0], accessor.getMessageHeaders());
  }

}
