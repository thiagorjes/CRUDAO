package com.crudao.kanban.domain.rbac;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Mapeamento manual (sem MapStruct): {@link Papel} tem uma coleção de {@link Permissao} que precisa
 * ser reduzida às chaves em {@link PapelDTO}, fora do escopo de um mapper gerado simples.
 */
@Component
public class PapelMapper {

  public PapelDTO paraDTO(Papel papel) {
    Set<String> chaves =
        papel.getPermissoes().stream().map(Permissao::getChave).collect(Collectors.toSet());
    return new PapelDTO(papel.getId(), papel.getNome(), papel.isProtegido(), chaves);
  }
}
