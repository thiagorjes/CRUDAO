package com.crudao.kanban.notificacao;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.notificacao.Notificacao;
import com.crudao.kanban.domain.notificacao.NotificacaoRepository;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaObservador;
import com.crudao.kanban.domain.tarefa.TarefaObservadorRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Notificações internas (RF-005) — geradas ao mudar etapa ou marcar/desmarcar impedimento de uma
 * tarefa, para responsável + criador + {@link TarefaObservador} explícitos.
 */
@Service
public class NotificacaoService {

    public static final String TIPO_TRANSICAO_ETAPA = "TRANSICAO_ETAPA";
    public static final String TIPO_IMPEDIMENTO_MARCADO = "IMPEDIMENTO_MARCADO";
    public static final String TIPO_IMPEDIMENTO_DESMARCADO = "IMPEDIMENTO_DESMARCADO";

    private final NotificacaoRepository notificacaoRepository;
    private final TarefaObservadorRepository tarefaObservadorRepository;
    private final NotificacaoPublisher notificacaoPublisher;

    public NotificacaoService(
            NotificacaoRepository notificacaoRepository,
            TarefaObservadorRepository tarefaObservadorRepository,
            NotificacaoPublisher notificacaoPublisher) {
        this.notificacaoRepository = notificacaoRepository;
        this.tarefaObservadorRepository = tarefaObservadorRepository;
        this.notificacaoPublisher = notificacaoPublisher;
    }

    /**
     * Cria uma {@link Notificacao} por observador da tarefa (responsável + criador + {@code
     * TarefaObservador}, deduplicados) e publica cada uma (canal por usuário, TASK-05.2). Chamado
     * dentro da mesma transação de escrita de {@code TarefaService.mover}/{@code
     * marcarImpedimento}/{@code desmarcarImpedimento} — a publicação real só ocorre após o commit
     * (ver {@code ListenNotifyNotificacaoPublisher}).
     */
    @Transactional
    public void notificarObservadores(Tarefa tarefa, String tipo, String mensagem) {
        OffsetDateTime agora = OffsetDateTime.now();
        for (UUID observadorId : resolverObservadores(tarefa)) {
            Notificacao notificacao = new Notificacao();
            notificacao.setUsuario(referenciaUsuario(observadorId));
            notificacao.setTarefa(tarefa);
            notificacao.setTipo(tipo);
            notificacao.setMensagem(mensagem);
            notificacao.setLida(false);
            notificacao.setCriadoEm(agora);
            notificacao = notificacaoRepository.save(notificacao);
            notificacaoPublisher.publicar(notificacao);
        }
    }

    /** Responsável + criador + observadores explícitos, sem duplicatas nem nulos. */
    private Set<UUID> resolverObservadores(Tarefa tarefa) {
        Set<UUID> observadores = new LinkedHashSet<>();
        if (tarefa.getResponsavel() != null) {
            observadores.add(tarefa.getResponsavel().getId());
        }
        if (tarefa.getCriadoPor() != null) {
            observadores.add(tarefa.getCriadoPor().getId());
        }
        tarefaObservadorRepository
                .findByTarefaId(tarefa.getId())
                .forEach(o -> observadores.add(o.getUsuarioId()));
        return observadores;
    }

    private Usuario referenciaUsuario(UUID usuarioId) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        return usuario;
    }

    /** Lista de notificações do usuário autenticado (RF-005), mais recentes primeiro. */
    public List<NotificacaoResponse> listar() {
        Usuario usuario = usuarioAutenticado();
        return notificacaoRepository.findByUsuarioIdAndLidaFalseOrderByCriadoEmDesc(usuario.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Marca uma notificação como lida — 404 se não existir ou pertencer a outro usuário. */
    @Transactional
    public void marcarComoLida(UUID id) {
        Usuario usuario = usuarioAutenticado();
        Notificacao notificacao =
                notificacaoRepository
                        .findByIdAndUsuarioId(id, usuario.getId())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "notificação não encontrada"));
        notificacao.setLida(true);
        notificacaoRepository.save(notificacao);
    }

    private Usuario usuarioAutenticado() {
        Usuario usuario = UsuarioAutenticadoHolder.get();
        if (usuario == null) {
            throw new AccessDeniedException("Acesso negado");
        }
        return usuario;
    }

    private NotificacaoResponse toResponse(Notificacao notificacao) {
        return new NotificacaoResponse(
                notificacao.getId(),
                notificacao.getTarefa().getId(),
                notificacao.getTipo(),
                notificacao.getMensagem(),
                notificacao.isLida(),
                notificacao.getCriadoEm());
    }
}
