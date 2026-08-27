package com.crudao.kanban.websocket;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.websocket.WsTicket;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/ws-ticket} — autenticado normalmente via Bearer (resource server de {@code
 * /api/**}), emite o ticket de curta duração que o frontend usa só para abrir a conexão STOMP
 * (TASK-07.2).
 */
@RestController
public class WsTicketController {

    private final WsTicketService wsTicketService;

    public WsTicketController(WsTicketService wsTicketService) {
        this.wsTicketService = wsTicketService;
    }

    @PostMapping("/api/ws-ticket")
    public ResponseEntity<WsTicketResponse> emitir() {
        Usuario usuario = UsuarioAutenticadoHolder.get();
        if (usuario == null) {
            throw new UsernameNotFoundException("usuario nao resolvido no contexto do request");
        }
        WsTicket ticket = wsTicketService.emitir(usuario);
        return ResponseEntity.ok(new WsTicketResponse(ticket.getId().toString(), ticket.getExpiraEm()));
    }
}
