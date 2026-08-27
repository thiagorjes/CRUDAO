package com.crudao.kanban.domain.tarefa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaImpedimentoHistoricoRepository
        extends JpaRepository<TarefaImpedimentoHistorico, UUID> {

    List<TarefaImpedimentoHistorico> findByTarefaId(UUID tarefaId);

    /** Usado na exclusão da tarefa (RF-019) — FK não tem cascade. */
    void deleteByTarefaId(UUID tarefaId);

    /**
     * Base para o dashboard de tempo médio de impedimento por etapa (RF-007, RN-002, TASK-06.1) —
     * só ciclos fechados e com {@code etapa} conhecida (registros anteriores à V11 ficam de fora,
     * não há como reconstituir retroativamente). {@code etapa} pré-carregada.
     */
    @EntityGraph(attributePaths = "etapa")
    List<TarefaImpedimentoHistorico> findByTarefaProjetoIdAndEtapaIsNotNullAndDesmarcadoEmIsNotNull(
            UUID projetoId);
}
