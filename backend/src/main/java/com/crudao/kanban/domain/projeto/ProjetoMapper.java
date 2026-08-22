package com.crudao.kanban.domain.projeto;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProjetoMapper {
  ProjetoDTO paraDTO(Projeto projeto);
}
