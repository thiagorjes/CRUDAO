package com.crudao.kanban.domain.rbac;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PapelRepository extends JpaRepository<Papel, UUID> {

  Optional<Papel> findByNomeIgnoreCase(String nome);
}
