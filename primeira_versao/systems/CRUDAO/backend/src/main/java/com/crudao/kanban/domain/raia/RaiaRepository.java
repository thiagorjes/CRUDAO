package com.crudao.kanban.domain.raia;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RaiaRepository extends JpaRepository<Raia, UUID> {
  List<Raia> findByProjetoIdOrderByOrdemAsc(UUID projetoId);

  List<Raia> findByProjetoIsNullOrderByOrdemAsc();
}
