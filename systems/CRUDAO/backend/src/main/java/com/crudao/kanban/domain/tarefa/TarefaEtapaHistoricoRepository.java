package com.crudao.kanban.domain.tarefa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaEtapaHistoricoRepository extends JpaRepository<TarefaEtapaHistorico, UUID> {

    /** Registro aberto (etapa em andamento) da tarefa — RF-006, RN-001. */
    Optional<TarefaEtapaHistorico> findByTarefaIdAndSaidaEmIsNull(UUID tarefaId);

    List<TarefaEtapaHistorico> findByTarefaIdOrderByEntradaEm(UUID tarefaId);
}
