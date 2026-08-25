package com.crudao.kanban.tarefa;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaObservador;
import com.crudao.kanban.domain.tarefa.TarefaObservadorRepository;
import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * CRUD de {@link TarefaObservador} (RF-005, TASK-05.2) — observador explícito além de responsável
 * e criador (que já observam implicitamente, ver {@code NotificacaoService}).
 *
 * <p>Qualquer membro do projeto pode se adicionar/remover a si mesmo como observador; adicionar ou
 * remover outro usuário exige {@code tarefa:gerenciar} — decisão de implementação (não
 * explicitada no contrato), consistente com o padrão de auto-atribuição livre + gestão restrita já
 * usado em {@code TarefaService.editar} (RN-012).
 */
@Service
public class TarefaObservadorService {

    private static final String PERMISSAO_GERENCIAR = "tarefa:gerenciar";

    private final TarefaObservadorRepository tarefaObservadorRepository;
    private final TarefaRepository tarefaRepository;
    private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
    private final PermissaoGuard permissaoGuard;

    public TarefaObservadorService(
            TarefaObservadorRepository tarefaObservadorRepository,
            TarefaRepository tarefaRepository,
            UsuarioProjetoPapelRepository usuarioProjetoPapelRepository,
            PermissaoGuard permissaoGuard) {
        this.tarefaObservadorRepository = tarefaObservadorRepository;
        this.tarefaRepository = tarefaRepository;
        this.usuarioProjetoPapelRepository = usuarioProjetoPapelRepository;
        this.permissaoGuard = permissaoGuard;
    }

    @Transactional
    public void adicionar(UUID tarefaId, UUID usuarioId) {
        Tarefa tarefa = buscarTarefa(tarefaId);
        UUID projetoId = tarefa.getProjeto().getId();
        exigirMembro(projetoId);
        Usuario autor = exigirPermissaoSobreAlvo(projetoId, usuarioId);

        if (usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(usuarioId, projetoId).isEmpty()
                && !usuarioId.equals(autor.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "usuário não vinculado ao projeto");
        }

        if (!tarefaObservadorRepository.existsByTarefaIdAndUsuarioId(tarefaId, usuarioId)) {
            tarefaObservadorRepository.save(new TarefaObservador(tarefaId, usuarioId));
        }
    }

    @Transactional
    public void remover(UUID tarefaId, UUID usuarioId) {
        Tarefa tarefa = buscarTarefa(tarefaId);
        UUID projetoId = tarefa.getProjeto().getId();
        exigirMembro(projetoId);
        exigirPermissaoSobreAlvo(projetoId, usuarioId);

        tarefaObservadorRepository.deleteByTarefaIdAndUsuarioId(tarefaId, usuarioId);
    }

    public List<UUID> listar(UUID tarefaId) {
        Tarefa tarefa = buscarTarefa(tarefaId);
        exigirMembro(tarefa.getProjeto().getId());
        return tarefaObservadorRepository.findByTarefaId(tarefaId).stream()
                .map(TarefaObservador::getUsuarioId)
                .toList();
    }

    /** Retorna o autor autenticado após validar que ele pode agir sobre {@code usuarioId}. */
    private Usuario exigirPermissaoSobreAlvo(UUID projetoId, UUID usuarioId) {
        Usuario autor = UsuarioAutenticadoHolder.get();
        if (autor == null) {
            throw new AccessDeniedException("Acesso negado");
        }
        if (!usuarioId.equals(autor.getId()) && !permissaoGuard.permitido(projetoId, PERMISSAO_GERENCIAR)) {
            throw new AccessDeniedException("Acesso negado");
        }
        return autor;
    }

    private void exigirMembro(UUID projetoId) {
        if (!permissaoGuard.membro(projetoId)) {
            throw new AccessDeniedException("Acesso negado");
        }
    }

    private Tarefa buscarTarefa(UUID id) {
        return tarefaRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "tarefa não encontrada"));
    }
}
