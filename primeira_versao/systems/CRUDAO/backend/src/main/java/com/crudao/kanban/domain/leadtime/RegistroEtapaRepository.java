package com.crudao.kanban.domain.leadtime;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroEtapaRepository extends JpaRepository<RegistroEtapa, UUID> {

  Optional<RegistroEtapa> findByTarefaIdAndSaidaEmIsNull(UUID tarefaId);

  List<RegistroEtapa> findByTarefaIdOrderByEntradaEmAsc(UUID tarefaId);

  List<RegistroEtapa> findByTarefaProjetoIdAndSaidaEmIsNotNullAndEntradaEmBetween(
      UUID projetoId, Instant dataInicio, Instant dataFim);
}
