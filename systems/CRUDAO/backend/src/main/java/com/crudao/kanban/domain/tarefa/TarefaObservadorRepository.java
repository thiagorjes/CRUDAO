package com.crudao.kanban.domain.tarefa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaObservadorRepository
        extends JpaRepository<TarefaObservador, TarefaObservador.Pk> {

    /** Usado na exclusão da tarefa (RF-019) — FK não tem cascade. */
    void deleteByTarefaId(UUID tarefaId);

    /** Observadores explícitos da tarefa (RF-005, TASK-05.2) — além de responsável e criador. */
    List<TarefaObservador> findByTarefaId(UUID tarefaId);

    void deleteByTarefaIdAndUsuarioId(UUID tarefaId, UUID usuarioId);

    boolean existsByTarefaIdAndUsuarioId(UUID tarefaId, UUID usuarioId);
}
