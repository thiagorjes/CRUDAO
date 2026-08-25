package com.crudao.kanban.domain.tarefa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaImpedimentoHistoricoRepository
        extends JpaRepository<TarefaImpedimentoHistorico, UUID> {

    List<TarefaImpedimentoHistorico> findByTarefaId(UUID tarefaId);

    /** Usado na exclusão da tarefa (RF-019) — FK não tem cascade. */
    void deleteByTarefaId(UUID tarefaId);
}
