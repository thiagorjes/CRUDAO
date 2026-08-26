package com.crudao.kanban.websocket;

import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.websocket.WsTicket;
import com.crudao.kanban.domain.websocket.WsTicketRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emissão/validação do ticket de curta duração usado para autenticar o handshake STOMP/SockJS a
 * partir do browser (TASK-07.2) — o access token real nunca sai do servidor Next.js.
 */
@Service
public class WsTicketService {

    private static final Logger log = LoggerFactory.getLogger(WsTicketService.class);
    private static final long TTL_SEGUNDOS = 20;

    private final WsTicketRepository wsTicketRepository;

    public WsTicketService(WsTicketRepository wsTicketRepository) {
        this.wsTicketRepository = wsTicketRepository;
    }

    @Transactional
    public WsTicket emitir(Usuario usuario) {
        WsTicket ticket = new WsTicket();
        ticket.setUsuario(usuario);
        ticket.setCriadoEm(OffsetDateTime.now());
        ticket.setExpiraEm(OffsetDateTime.now().plusSeconds(TTL_SEGUNDOS));
        ticket.setUsado(false);
        return wsTicketRepository.save(ticket);
    }

    /** Uso único — marca o ticket como usado mesmo quando ele já expirou (nunca reaproveitável). */
    @Transactional
    public Optional<Usuario> validarEUsar(UUID ticketId) {
        Optional<WsTicket> encontrado = wsTicketRepository.findById(ticketId);
        if (encontrado.isEmpty()) {
            return Optional.empty();
        }

        WsTicket ticket = encontrado.get();
        boolean valido = !ticket.isUsado() && ticket.getExpiraEm().isAfter(OffsetDateTime.now());

        ticket.setUsado(true);
        wsTicketRepository.save(ticket);

        return valido ? Optional.of(ticket.getUsuario()) : Optional.empty();
    }

    /**
     * Purga tickets expirados (usados ou não) — sem isso {@code ws_ticket} cresce indefinidamente,
     * uma linha por conexão/reconexão de board (achado de code review, TASK-07.2). Expirados nunca
     * são reaproveitáveis (checagem de {@code expiraEm} em {@link #validarEUsar}), então apagar não
     * afeta nenhuma sessão em andamento.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void limparExpirados() {
        long removidos = wsTicketRepository.deleteByExpiraEmBefore(OffsetDateTime.now());
        if (removidos > 0) {
            log.debug("ws_ticket: {} ticket(s) expirado(s) removido(s)", removidos);
        }
    }
}
