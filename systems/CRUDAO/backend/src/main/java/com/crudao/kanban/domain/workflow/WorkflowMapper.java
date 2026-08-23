package com.crudao.kanban.domain.workflow;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkflowMapper {

  WorkflowDTO paraDTO(Workflow workflow);

  @Mapping(target = "workflowId", source = "workflow.id")
  EtapaDTO paraDTO(Etapa etapa);

  @Mapping(target = "etapaOrigemId", source = "etapaOrigem.id")
  @Mapping(target = "etapaDestinoId", source = "etapaDestino.id")
  TransicaoDTO paraDTO(Transicao transicao);
}
