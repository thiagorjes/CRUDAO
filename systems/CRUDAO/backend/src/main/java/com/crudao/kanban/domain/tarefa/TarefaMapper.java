package com.crudao.kanban.domain.tarefa;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TarefaMapper {

  @Mapping(target = "projetoId", source = "projeto.id")
  @Mapping(target = "workflowId", source = "workflow.id")
  @Mapping(target = "etapaAtualId", source = "etapaAtual.id")
  @Mapping(target = "raiaId", source = "raia.id")
  TarefaDTO paraDTO(Tarefa tarefa);
}
