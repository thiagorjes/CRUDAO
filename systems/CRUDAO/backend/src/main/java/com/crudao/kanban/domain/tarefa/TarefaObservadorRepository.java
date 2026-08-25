package com.crudao.kanban.domain.tarefa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaObservadorRepository
        extends JpaRepository<TarefaObservador, TarefaObservador.Pk> {

    /** Usado na exclusão da tarefa (RF-019) — FK não tem cascade. */
    void deleteByTarefaId(UUID tarefaId);
}
