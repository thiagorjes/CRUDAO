package com.crudao.kanban.domain.workflow;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtapaRepository extends JpaRepository<Etapa, UUID> {
    List<Etapa> findByWorkflowIdOrderByOrdemAsc(UUID workflowId);
}

