package com.crudao.kanban.domain.raia;

import java.util.List;

/**
 * Resolve quais raias um projeto deve exibir no board — RF-011.
 *
 * <p>Regra (PRD v1.1, clarificação): projeto sem raias próprias usa as raias default globais.
 */
public final class RaiaResolver {

  private RaiaResolver() {}

  public static List<Raia> resolver(List<Raia> raiasDoProjeto, List<Raia> raiasDefaultGlobais) {
    return raiasDoProjeto.isEmpty() ? raiasDefaultGlobais : raiasDoProjeto;
  }
}
