package com.crudao.kanban.domain.workflow;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

    List<Workflow> findByProjetoId(UUID projetoId);
}
