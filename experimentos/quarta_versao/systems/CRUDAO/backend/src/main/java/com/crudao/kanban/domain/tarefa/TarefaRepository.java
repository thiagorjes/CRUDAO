package com.crudao.kanban.domain.tarefa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaRepository extends JpaRepository<Tarefa, UUID> {

    List<Tarefa> findByProjetoId(UUID projetoId);

    boolean existsByWorkflowId(UUID workflowId);

    boolean existsByEtapaAtualId(UUID etapaId);

    boolean existsByRaiaId(UUID raiaId);
}

