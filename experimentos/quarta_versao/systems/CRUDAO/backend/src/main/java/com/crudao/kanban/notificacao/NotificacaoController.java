package com.crudao.kanban.notificacao;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.notificacao.Notificacao;
import com.crudao.kanban.domain.notificacao.NotificacaoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * TASK-05.2: Endpoints de notificações internas.
 *
 * RF-005: Notificações internas para observadores de tarefas.
 * RNF-003: Validação de permissão no backend (nunca confiar na UI).
 */
@Slf4j
@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService notificacaoService;
    private final NotificacaoRepository notificacaoRepository;

    /**
     * GET /api/notificacoes
     * Retorna notificações não lidas do usuário autenticado.
     * RF-005: Lista de notificações não lidas.
     */
    @GetMapping
    public ResponseEntity<List<NotificacaoResponse>> obterNaoLidas() {
        Usuario usuario = UsuarioAutenticadoHolder.get();
        if (usuario == null || usuario.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        List<Notificacao> notificacoes = notificacaoService.obterNaoLidas(usuario.getId());

        List<NotificacaoResponse> response = notificacoes.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/notificacoes/{id}/marcar-como-lida
     * Marca notificação como lida.
     * RF-005: Ação do usuário sobre notificação.
     *
     * RNF-003: Valida que notificação pertence ao usuário autenticado.
     */
    @PutMapping("/{id}/marcar-como-lida")
    public ResponseEntity<Void> marcarComoLida(@PathVariable UUID id) {
        Usuario usuario = UsuarioAutenticadoHolder.get();
        if (usuario == null || usuario.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        // Valida autorização: notificação deve pertencer ao usuário autenticado
        notificacaoService.marcarComoLidaComAutorizacao(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }

    // DTO auxiliar
    private NotificacaoResponse toResponse(Notificacao notif) {
        return new NotificacaoResponse(
            notif.getId(),
            notif.getTarefa().getId(),
            notif.getTarefa().getTitulo(),
            notif.getTipo().toString(),
            notif.isLida(),
            notif.getCriadoEm()
        );
    }

    record NotificacaoResponse(
            UUID id,
            UUID tarefaId,
            String tarefaTitulo,
            String tipo,
            boolean lida,
            java.time.Instant criadoEm
    ) {}
}
