package com.crudao.kanban.domain.rbac;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissaoRepository extends JpaRepository<Permissao, UUID> {

  Optional<Permissao> findByChave(String chave);
}
