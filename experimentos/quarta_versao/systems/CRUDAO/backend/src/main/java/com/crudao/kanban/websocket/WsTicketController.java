package com.crudao.kanban.websocket;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.usuario.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * TASK-07.7: emite tickets de curta duração para o handshake WebSocket.
 *
 * <p>Autenticado como qualquer {@code /api/**} (Bearer via proxy do BFF). Consumido pelos clientes
 * STOMP (board e notificações) antes de abrir a conexão.
 */
@RestController
@RequestMapping("/api/ws-ticket")
@RequiredArgsConstructor
public class WsTicketController {

    private final WsTicketService wsTicketService;

    @PostMapping
    public ResponseEntity<WsTicketResponse> emitir() {
        Usuario usuario = UsuarioAutenticadoHolder.get();
        if (usuario == null || usuario.getEmail() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        String ticket = wsTicketService.emitir(usuario.getEmail());
        return ResponseEntity.ok(new WsTicketResponse(ticket, WsTicketService.TTL_SEGUNDOS));
    }

    record WsTicketResponse(String ticket, long expiraEmSegundos) {}
}
