package com.crudao.kanban.domain.tarefa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaEtapaHistoricoRepository extends JpaRepository<TarefaEtapaHistorico, UUID> {
    List<TarefaEtapaHistorico> findByTarefaIdOrderByEntradaEmAsc(UUID tarefaId);

    List<TarefaEtapaHistorico> findByTarefa_Projeto_Id(UUID projetoId);
}

