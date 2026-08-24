package com.crudao.kanban.domain.tarefa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObservadorRepository extends JpaRepository<Observador, UUID> {

  List<Observador> findByTarefaId(UUID tarefaId);

  void deleteByTarefaIdAndUsuarioId(UUID tarefaId, UUID usuarioId);

  boolean existsByTarefaIdAndUsuarioId(UUID tarefaId, UUID usuarioId);
}
