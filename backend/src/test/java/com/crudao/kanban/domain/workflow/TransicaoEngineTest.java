package com.crudao.kanban.domain.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransicaoEngineTest {

  private final TransicaoEngine engine = new TransicaoEngine();

  private Etapa etapa(String nome, boolean etapaFinal) {
    Etapa etapa = new Etapa();
    etapa.setId(UUID.randomUUID());
    etapa.setNome(nome);
    etapa.setEtapaFinal(etapaFinal);
    return etapa;
  }

  private Transicao transicao(Etapa origem, Etapa destino, TipoTransicao tipo) {
    return new Transicao(UUID.randomUUID(), origem, destino, tipo);
  }

  @Test
  void permiteMovimentoQuandoExisteTransicaoNormalConfigurada() {
    Etapa backlog = etapa("Backlog", false);
    Etapa emAndamento = etapa("Em Andamento", false);
    Transicao t = transicao(backlog, emAndamento, TipoTransicao.NORMAL);

    boolean permitido = engine.transicaoPermitida(backlog, emAndamento, List.of(t));

    assertThat(permitido).isTrue();
  }

  @Test
  void proibeMovimentoQuandoNaoExisteTransicaoConfigurada() {
    Etapa backlog = etapa("Backlog", false);
    Etapa concluido = etapa("Concluído", true);

    boolean permitido = engine.transicaoPermitida(backlog, concluido, List.of());

    assertThat(permitido).isFalse();
  }

  @Test
  void permiteReaberturaDeEtapaFinalQuandoTransicaoDoTipoReaberturaExiste() {
    Etapa concluido = etapa("Concluído", true);
    Etapa emRevisao = etapa("Em Revisão", false);
    Transicao reabertura = transicao(concluido, emRevisao, TipoTransicao.REABERTURA);

    boolean permitido = engine.transicaoPermitida(concluido, emRevisao, List.of(reabertura));

    assertThat(permitido).isTrue();
  }

  @Test
  void proibeReaberturaQuandoNenhumaTransicaoDoTipoReaberturaExisteParaEtapaFinal() {
    Etapa concluido = etapa("Concluído", true);
    Etapa emRevisao = etapa("Em Revisão", false);

    boolean permitido = engine.transicaoPermitida(concluido, emRevisao, List.of());

    assertThat(permitido).isFalse();
  }

  @Test
  void listaEtapasDestinoValidasAPartirDaOrigem() {
    Etapa backlog = etapa("Backlog", false);
    Etapa emAndamento = etapa("Em Andamento", false);
    Etapa emRevisao = etapa("Em Revisão", false);
    Transicao paraEmAndamento = transicao(backlog, emAndamento, TipoTransicao.NORMAL);
    Transicao irrelevante = transicao(emAndamento, emRevisao, TipoTransicao.NORMAL);

    List<Etapa> destinosValidos =
        engine.destinosValidos(backlog, List.of(paraEmAndamento, irrelevante));

    assertThat(destinosValidos).containsExactly(emAndamento);
  }

  @Test
  void naoPermiteTransicaoParaSiMesma() {
    Etapa backlog = etapa("Backlog", false);
    Transicao autoTransicao = transicao(backlog, backlog, TipoTransicao.NORMAL);

    boolean permitido = engine.transicaoPermitida(backlog, backlog, List.of(autoTransicao));

    assertThat(permitido).isFalse();
  }
}
