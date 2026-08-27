package com.crudao.kanban.domain.workflow;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransicaoRepository extends JpaRepository<Transicao, UUID> {

  List<Transicao> findByEtapaOrigemId(UUID etapaOrigemId);

  @Query(
      "select t from Transicao t where t.etapaOrigem.workflow.id = :workflowId or"
          + " t.etapaDestino.workflow.id = :workflowId")
  List<Transicao> findByWorkflowId(@Param("workflowId") UUID workflowId);
}
