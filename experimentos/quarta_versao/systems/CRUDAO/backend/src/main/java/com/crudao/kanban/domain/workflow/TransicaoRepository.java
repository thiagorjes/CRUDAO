package com.crudao.kanban.domain.workflow;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransicaoRepository extends JpaRepository<Transicao, UUID> {
    List<Transicao> findByEtapaOrigemId(UUID etapaOrigemId);
    void deleteByEtapaOrigemId(UUID etapaOrigemId);
    List<Transicao> findByEtapaOrigemWorkflowId(UUID workflowId);
}

