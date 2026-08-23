package com.crudao.kanban.domain.tarefa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaRepository extends JpaRepository<Tarefa, UUID> {

  List<Tarefa> findByProjetoIdOrderByCriadoEmAsc(UUID projetoId);

  boolean existsByEtapaAtualId(UUID etapaId);

  boolean existsByRaiaId(UUID raiaId);

  boolean existsByProjetoId(UUID projetoId);
}
