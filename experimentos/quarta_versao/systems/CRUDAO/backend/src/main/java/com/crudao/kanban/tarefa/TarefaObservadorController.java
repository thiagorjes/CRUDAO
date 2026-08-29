package com.crudao.kanban.tarefa;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaObservador;
import com.crudao.kanban.domain.tarefa.TarefaObservadorId;
import com.crudao.kanban.domain.tarefa.TarefaObservadorRepository;
import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * TASK-05.2: Endpoints de gerenciamento de observadores de tarefas.
 *
 * RF-005: Notificações internas — CRUD de TarefaObservador.
 * RNF-003: Validação de permissão no backend.
 */
@Slf4j
@RestController
@RequestMapping("/api/tarefas/{tarefaId}/observadores")
@RequiredArgsConstructor
public class TarefaObservadorController {

    private final TarefaRepository tarefaRepository;
    private final TarefaObservadorRepository tarefaObservadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final PermissaoGuard permissaoGuard;

    /**
     * POST /api/tarefas/{tarefaId}/observadores/{usuarioId}
     * Adiciona observador explícito a uma tarefa.
     */
    @PostMapping("/{usuarioId}")
    public ResponseEntity<Void> adicionarObservador(
            @PathVariable UUID tarefaId,
            @PathVariable UUID usuarioId) {

        Usuario usuario = UsuarioAutenticadoHolder.get();
        if (usuario == null || usuario.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        Tarefa tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));

        // Valida permissão (usuário precisa ter tarefa:gerenciar no projeto)
        permissaoGuard.exigir(tarefa.getProjeto().getId(), "tarefa:gerenciar");

        Usuario observador = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        // Verifica se já é observador
        TarefaObservadorId id = new TarefaObservadorId(tarefaId, usuarioId);
        if (tarefaObservadorRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuário já é observador desta tarefa");
        }

        TarefaObservador obs = new TarefaObservador();
        obs.setId(id);
        obs.setTarefa(tarefa);
        obs.setUsuario(observador);
        tarefaObservadorRepository.save(obs);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * DELETE /api/tarefas/{tarefaId}/observadores/{usuarioId}
     * Remove observador explícito de uma tarefa.
     */
    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<Void> removerObservador(
            @PathVariable UUID tarefaId,
            @PathVariable UUID usuarioId) {

        Usuario usuario = UsuarioAutenticadoHolder.get();
        if (usuario == null || usuario.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        Tarefa tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));

        // Valida permissão
        permissaoGuard.exigir(tarefa.getProjeto().getId(), "tarefa:gerenciar");

        TarefaObservadorId id = new TarefaObservadorId(tarefaId, usuarioId);
        if (!tarefaObservadorRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Observador não encontrado");
        }

        tarefaObservadorRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
