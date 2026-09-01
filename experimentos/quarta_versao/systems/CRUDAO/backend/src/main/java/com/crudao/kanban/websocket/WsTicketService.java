package com.crudao.kanban.websocket;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Emite e valida tickets de curta duração para autenticar o handshake WebSocket (TASK-07.7).
 *
 * <p>O browser não consegue enviar {@code Authorization: Bearer} no handshake nativo de WebSocket e
 * o BFF do Next.js nunca expõe o access token ao JS. O fluxo é: (1) o cliente chama {@code POST
 * /api/ws-ticket} autenticado normalmente (Bearer via proxy do BFF); (2) recebe um ticket assinado
 * (HMAC-SHA256) com validade de {@value #TTL_SEGUNDOS}s carregando o e-mail do usuário; (3) conecta
 * em {@code ws://.../ws?ticket=...}; (4) {@link WsTicketAuthenticationFilter} valida o ticket e
 * popula o {@code Principal} da sessão WebSocket.
 *
 * <p>Ticket é stateless (sem store) — assinatura + expiração curta bastam; compatível com múltiplos
 * pods sem estado local compartilhado (constituição, princípio 5).
 */
@Slf4j
@Service
public class WsTicketService {

    static final long TTL_SEGUNDOS = 30;
    /** Comprimento mínimo do segredo HMAC (>= 128 bits de material de chave). */
    static final int SECRET_MIN_LEN = 16;
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final byte[] secret;

    /**
     * @param secret valor de {@code kanban.ws-ticket.secret} — <b>sem default</b>: cada ambiente
     *     (incl. dev/test) declara o seu, e produção deve injetar {@code WS_TICKET_SECRET}. O boot
     *     falha se ausente, em branco ou curto demais — nunca há segredo público embutido (mesma
     *     regra do realm Keycloak).
     */
    public WsTicketService(@Value("${kanban.ws-ticket.secret:}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "kanban.ws-ticket.secret não configurado — defina a variável de ambiente "
                            + "WS_TICKET_SECRET (obrigatória para autenticar o handshake WebSocket).");
        }
        if (secret.strip().length() < SECRET_MIN_LEN) {
            throw new IllegalStateException(
                    "kanban.ws-ticket.secret muito curto (mínimo " + SECRET_MIN_LEN + " caracteres).");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** Emite um ticket assinado para o e-mail informado. */
    public String emitir(String email) {
        long expEpochSec = Instant.now().plus(Duration.ofSeconds(TTL_SEGUNDOS)).getEpochSecond();
        String payload = email + "|" + expEpochSec;
        String payloadB64 = B64.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String assinatura = B64.encodeToString(hmac(payloadB64));
        return payloadB64 + "." + assinatura;
    }

    /** Valida o ticket; devolve o e-mail se a assinatura confere e o ticket não expirou. */
    public Optional<String> validar(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return Optional.empty();
        }
        int ponto = ticket.indexOf('.');
        if (ponto <= 0 || ponto == ticket.length() - 1) {
            return Optional.empty();
        }
        String payloadB64 = ticket.substring(0, ponto);
        String assinatura = ticket.substring(ponto + 1);

        String esperada = B64.encodeToString(hmac(payloadB64));
        if (!constantTimeEquals(esperada, assinatura)) {
            log.debug("Ticket WS com assinatura inválida");
            return Optional.empty();
        }

        String payload;
        try {
            payload = new String(B64D.decode(payloadB64), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        int sep = payload.lastIndexOf('|');
        if (sep <= 0) {
            return Optional.empty();
        }
        String email = payload.substring(0, sep);
        long expEpochSec;
        try {
            expEpochSec = Long.parseLong(payload.substring(sep + 1));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        if (Instant.now().getEpochSecond() > expEpochSec) {
            log.debug("Ticket WS expirado para {}", email);
            return Optional.empty();
        }
        return Optional.of(email);
    }

    private byte[] hmac(String dados) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret, HMAC_ALGO));
            return mac.doFinal(dados.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular HMAC do ticket WS", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] ba = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (ba.length != bb.length) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < ba.length; i++) {
            r |= ba[i] ^ bb[i];
        }
        return r == 0;
    }
}
