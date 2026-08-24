package com.crudao.kanban.domain.projeto;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoProjetoRepository extends JpaRepository<ConfiguracaoProjeto, UUID> {}
