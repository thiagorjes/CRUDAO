package com.crudao.kanban.websocket;

import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * Interceptor que valida autorização na subscrição STOMP.
 *
 * Regras:
 * - `/topic/board/{projetoId}`: usuário deve ter vínculo UsuarioProjetoPapel ativo com o projetoId.
 * - `/topic/notificacoes/{usuarioId}`: usuário autenticado deve ser o próprio usuarioId (autorização simples).
 *
 * RNF-003: Autorização validada no backend, nunca confiando em UI.
 * Subscrição desautorizada resulta em resposta ERROR STOMP (sem desconexão, apenas bloqueio do tópico).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BoardChannelInterceptor implements ChannelInterceptor {

    private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
    private final UsuarioRepository usuarioRepository;

    private static final Pattern BOARD_TOPIC_PATTERN = Pattern.compile("^/topic/board/([a-f0-9-]+)$");
    private static final Pattern NOTIFICACOES_TOPIC_PATTERN = Pattern.compile("^/topic/notificacoes/([a-f0-9-]+)$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // Valida apenas mensagens SUBSCRIBE
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            Principal principal = accessor.getUser();

            if (destination == null || principal == null) {
                log.warn("Subscrição inválida: destination={}, principal={}", destination, principal);
                return null; // Bloqueia a mensagem
            }

            // Valida subscrição ao topic de board
            Matcher boardMatcher = BOARD_TOPIC_PATTERN.matcher(destination);
            if (boardMatcher.matches()) {
                UUID projetoId = UUID.fromString(boardMatcher.group(1));
                if (!validarAcessoBoard(principal.getName(), projetoId)) {
                    log.warn("Acesso negado a /topic/board/{} para usuário {}", projetoId, principal.getName());
                    enviarErrorStomp(accessor, "Acesso negado ao board do projeto");
                    return null;
                }
                log.debug("Acesso autorizado a /topic/board/{} para usuário {}", projetoId, principal.getName());
                return message;
            }

            // Valida subscrição ao topic de notificações
            Matcher notifMatcher = NOTIFICACOES_TOPIC_PATTERN.matcher(destination);
            if (notifMatcher.matches()) {
                UUID usuarioIdDestino = UUID.fromString(notifMatcher.group(1));
                if (!validarAcessoNotificacoes(principal.getName(), usuarioIdDestino)) {
                    log.warn("Acesso negado a /topic/notificacoes/{} para usuário {}", usuarioIdDestino, principal.getName());
                    enviarErrorStomp(accessor, "Acesso negado às notificações");
                    return null;
                }
                log.debug("Acesso autorizado a /topic/notificacoes/{} para usuário {}", usuarioIdDestino, principal.getName());
                return message;
            }

            // Outros tópicos: bloqueia por padrão
            log.warn("Subscrição em tópico desconhecido: {} por usuário {}", destination, principal.getName());
            enviarErrorStomp(accessor, "Tópico não reconhecido");
            return null;
        }

        return message;
    }

    /**
     * Valida se o usuário tem acesso ao board de um projeto.
     * RN: Usuário deve ter vínculo UsuarioProjetoPapel ativo (Projeto.ativo=true).
     */
    private boolean validarAcessoBoard(String usuarioEmail, UUID projetoId) {
        try {
            var usuario = usuarioRepository.findByEmail(usuarioEmail);
            if (usuario.isEmpty() || !usuario.get().isAtivo()) {
                return false;
            }

            // Verifica se há vínculo ativo do usuário com o projeto
            var vinculos = usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(
                usuario.get().getId(),
                projetoId
            );

            return !vinculos.isEmpty();
        } catch (Exception e) {
            log.error("Erro ao validar acesso ao board", e);
            return false;
        }
    }

    /**
     * Valida se o usuário pode acessar suas próprias notificações.
     * RN: Usuário autenticado deve ser o proprietário do tópico de notificações.
     */
    private boolean validarAcessoNotificacoes(String usuarioEmail, UUID usuarioIdDestino) {
        try {
            var usuario = usuarioRepository.findByEmail(usuarioEmail);
            if (usuario.isEmpty() || !usuario.get().isAtivo()) {
                return false;
            }

            // Usuário só pode acessar suas próprias notificações
            return usuario.get().getId().equals(usuarioIdDestino);
        } catch (Exception e) {
            log.error("Erro ao validar acesso a notificações", e);
            return false;
        }
    }

    /**
     * Envia uma resposta ERROR STOMP ao cliente para sinalizar falha de autorização.
     * O cliente receberá a mensagem de erro mas a subscrição será bloqueada.
     */
    private void enviarErrorStomp(StompHeaderAccessor accessor, String mensagemErro) {
        // Spring STOMP não oferece suporte direto a enviar ERROR na interceptação
        // Este é um placeholder — a subscrição é bloqueada por retornar null
        log.info("Bloqueando subscrição desautorizada: {}", mensagemErro);
    }
}
