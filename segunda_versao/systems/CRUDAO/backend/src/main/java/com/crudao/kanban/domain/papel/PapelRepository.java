package com.crudao.kanban.domain.papel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PapelRepository extends JpaRepository<Papel, UUID> {

    Optional<Papel> findByChaveAndProjetoIsNull(String chave);

    List<Papel> findByProjetoId(UUID projetoId);

    Optional<Papel> findByProjetoIdAndChave(UUID projetoId, String chave);
}
