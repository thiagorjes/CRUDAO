package com.crudao.kanban.domain.raia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RaiaResolverTest {

  @Test
  void deveRetornarRaiasDoProjetoQuandoExistirem() {
    Raia raiaDoProjeto = new Raia();
    raiaDoProjeto.setNome("Dev A");
    List<Raia> raiasDoProjeto = List.of(raiaDoProjeto);
    List<Raia> raiasDefaultGlobais = List.of(new Raia());

    List<Raia> resultado = RaiaResolver.resolver(raiasDoProjeto, raiasDefaultGlobais);

    assertThat(resultado).containsExactly(raiaDoProjeto);
  }

  @Test
  void deveRetornarRaiasDefaultGlobaisQuandoProjetoNaoTiverRaiasProprias() {
    List<Raia> raiasDoProjeto = List.of();
    Raia raiaDefault = new Raia();
    raiaDefault.setNome("Default");
    List<Raia> raiasDefaultGlobais = List.of(raiaDefault);

    List<Raia> resultado = RaiaResolver.resolver(raiasDoProjeto, raiasDefaultGlobais);

    assertThat(resultado).containsExactly(raiaDefault);
  }

  @Test
  void deveRetornarListaVaziaQuandoNaoHouverRaiasNemDefault() {
    List<Raia> resultado = RaiaResolver.resolver(List.of(), List.of());

    assertThat(resultado).isEmpty();
  }
}
