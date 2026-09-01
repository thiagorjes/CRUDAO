package com.crudao.kanban.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.crudao.kanban.domain.papel.Papel;
import com.crudao.kanban.domain.papel.PapelRepository;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapel;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.evento.EventoBoardPublisher.EventoBoardPayload;
import com.crudao.kanban.evento.adapter.ListenNotifyPublisher;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * TASK-07.7 — smoke ponta a ponta do handshake WebSocket contra o stack Docker final.
 *
 * <p>Sobe o servlet container real ({@code RANDOM_PORT}) e um cliente STOMP nativo ({@code new
 * WebSocket()}, sem SockJS) exercitando toda a cadeia real: {@link WsTicketService} emite ticket →
 * handshake em {@code /ws?ticket=} passa pelo {@link WsTicketAuthenticationFilter} e pela {@code
 * SecurityFilterChain} do resource server → {@link BoardChannelInterceptor} autoriza o SUBSCRIBE
 * por vínculo RBAC → evento publicado via {@link ListenNotifyPublisher} (NOTIFY no Postgres {@code
 * kanban_it}) é retransmitido pelo SimpleBroker e entregue ao cliente.
 *
 * <p>Casos cobertos: (1) ticket válido + vínculo → evento entregue &lt;2s (RNF-001); (2) ticket
 * inválido → handshake recusado (401); (3) ticket válido sem vínculo ao projeto → SUBSCRIBE
 * silenciosamente bloqueado, nenhum evento entregue (RNF-003, fail-closed).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("TASK-07.7: handshake WebSocket ponta a ponta (ticket + SUBSCRIBE + broadcast)")
class WsHandshakeIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private WsTicketService wsTicketService;
  @Autowired private ListenNotifyPublisher listenNotifyPublisher;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private ProjetoRepository projetoRepository;
  @Autowired private PapelRepository papelRepository;
  @Autowired private UsuarioProjetoPapelRepository uppRepository;

  private static final String EMAIL = "ws-smoke@crudao.local";

  private Usuario usuario;
  private Projeto projeto;
  private Papel papel;
  private WebSocketStompClient stompClient;

  @BeforeEach
  void seed() {
    limpar();

    usuario = new Usuario();
    usuario.setKeycloakSub("ws-smoke-" + UUID.randomUUID());
    usuario.setNome("WS Smoke");
    usuario.setEmail(EMAIL);
    usuario.setAtivo(true);
    usuario = usuarioRepository.save(usuario);

    projeto = new Projeto();
    projeto.setNome("WS Smoke Projeto");
    projeto.setDescricao("Projeto do smoke de handshake WS");
    projeto.setStatus(Projeto.Status.ATIVO);
    projeto.setCriadoPor(usuario);
    projeto.setCriadoEm(OffsetDateTime.now());
    projeto = projetoRepository.save(projeto);

    papel = new Papel();
    papel.setProjeto(projeto);
    papel.setChave("membro");
    papel.setNome("Membro");
    papel = papelRepository.save(papel);

    uppRepository.save(new UsuarioProjetoPapel(usuario, projeto, papel, OffsetDateTime.now()));

    stompClient = new WebSocketStompClient(new StandardWebSocketClient());
    stompClient.setMessageConverter(new MappingJackson2MessageConverter());
  }

  @AfterEach
  void tearDown() {
    if (stompClient != null) {
      stompClient.stop();
    }
    limpar();
  }

  private void limpar() {
    uppRepository
        .findByProjetoId(
            projetoRepository.findAll().stream()
                .filter(p -> "WS Smoke Projeto".equals(p.getNome()))
                .map(Projeto::getId)
                .findFirst()
                .orElse(new UUID(0, 0)))
        .forEach(uppRepository::delete);
    papelRepository.findAll().stream()
        .filter(p -> "membro".equals(p.getChave()) && p.getProjeto() != null)
        .filter(
            p ->
                projetoRepository
                    .findById(p.getProjeto().getId())
                    .map(pr -> "WS Smoke Projeto".equals(pr.getNome()))
                    .orElse(false))
        .forEach(papelRepository::delete);
    projetoRepository.findAll().stream()
        .filter(p -> "WS Smoke Projeto".equals(p.getNome()))
        .forEach(projetoRepository::delete);
    usuarioRepository.findByEmail(EMAIL).ifPresent(usuarioRepository::delete);
  }

  private String wsUrl(String ticket) {
    return "ws://localhost:" + port + "/ws?ticket=" + ticket;
  }

  private StompSession conectar(String ticket) throws Exception {
    return stompClient
        .connectAsync(wsUrl(ticket), new StompSessionHandlerAdapter() {})
        .get(5, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("ticket válido + vínculo → SUBSCRIBE aceito e evento de board entregue < 2s")
  void handshakeEBroadcastPontaAPonta() throws Exception {
    await().atMost(Duration.ofSeconds(10)).until(listenNotifyPublisher::isConectado);

    BlockingQueue<Map<String, Object>> recebidas = new LinkedBlockingQueue<>();
    StompSession session = conectar(wsTicketService.emitir(EMAIL));

    session.subscribe(
        "/topic/board/" + projeto.getId(),
        new StompFrameHandler() {
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return Map.class;
          }

          @Override
          @SuppressWarnings("unchecked")
          public void handleFrame(StompHeaders headers, Object payload) {
            recebidas.add((Map<String, Object>) payload);
          }
        });

    // pequena folga para o SUBSCRIBE ser processado pelo interceptor antes do NOTIFY
    Thread.sleep(300);

    listenNotifyPublisher.publicar(
        new EventoBoardPayload("TAREFA_MOVIDA", projeto.getId(), 1L, "{\"tarefaId\":\"x\"}"));

    Map<String, Object> frame = recebidas.poll(2, TimeUnit.SECONDS);
    assertThat(frame).as("evento deve chegar ao cliente STOMP em < 2s (RNF-001)").isNotNull();
    assertThat(((Map<?, ?>) frame.get("data")).get("tipo")).isEqualTo("TAREFA_MOVIDA");
    assertThat(((Map<?, ?>) frame.get("data")).get("projetoId"))
        .isEqualTo(projeto.getId().toString());
  }

  @Test
  @DisplayName("ticket inválido → handshake WS recusado com 401")
  void handshakeRecusadoComTicketInvalido() {
    assertThatThrownBy(() -> conectar("ticket-invalido.assinatura-falsa"))
        .as("handshake deve falhar quando o ticket não confere");
  }

  @Test
  @DisplayName("ticket válido sem vínculo ao projeto → SUBSCRIBE bloqueado, nenhum evento entregue")
  void subscribeBloqueadoSemVinculo() throws Exception {
    await().atMost(Duration.ofSeconds(10)).until(listenNotifyPublisher::isConectado);

    UUID projetoSemVinculo = UUID.randomUUID();
    BlockingQueue<Map<String, Object>> recebidas = new LinkedBlockingQueue<>();
    StompSession session = conectar(wsTicketService.emitir(EMAIL));

    session.subscribe(
        "/topic/board/" + projetoSemVinculo,
        new StompFrameHandler() {
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return Map.class;
          }

          @Override
          @SuppressWarnings("unchecked")
          public void handleFrame(StompHeaders headers, Object payload) {
            recebidas.add((Map<String, Object>) payload);
          }
        });

    Thread.sleep(300);

    listenNotifyPublisher.publicar(
        new EventoBoardPayload("TAREFA_MOVIDA", projetoSemVinculo, 1L, "{}"));

    assertThat(recebidas.poll(2, TimeUnit.SECONDS))
        .as("sem vínculo RBAC o SUBSCRIBE é bloqueado e nada é entregue")
        .isNull();
  }
}
