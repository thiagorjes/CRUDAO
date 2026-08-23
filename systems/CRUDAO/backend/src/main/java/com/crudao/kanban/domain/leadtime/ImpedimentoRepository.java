package com.crudao.kanban.domain.leadtime;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImpedimentoRepository extends JpaRepository<Impedimento, UUID> {

  Optional<Impedimento> findByRegistroEtapaIdAndFimEmIsNull(UUID registroEtapaId);
}
