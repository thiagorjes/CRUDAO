package com.crudao.kanban.domain.raia;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RaiaMapper {

  @Mapping(target = "projetoId", source = "projeto.id")
  RaiaDTO paraDTO(Raia raia);
}
