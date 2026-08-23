package com.crudao.kanban.domain.workflow;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Engine de validação de movimentação de tarefas entre etapas (RF-002).
 *
 * <p>Uma transição é permitida quando existe um registro {@link Transicao} ligando a etapa de
 * origem à etapa de destino — não importa se o tipo é {@link TipoTransicao#NORMAL} ou {@link
 * TipoTransicao#REABERTURA} (usado para "desfinalizar", RF-012). Transição para a própria etapa
 * nunca é permitida.
 */
@Component
public class TransicaoEngine {

  public boolean transicaoPermitida(Etapa origem, Etapa destino, List<Transicao> transicoes) {
    if (Objects.equals(origem.getId(), destino.getId())) {
      return false;
    }
    return transicoes.stream()
        .anyMatch(
            t ->
                Objects.equals(t.getEtapaOrigem().getId(), origem.getId())
                    && Objects.equals(t.getEtapaDestino().getId(), destino.getId()));
  }

  /** Lista as etapas de destino alcançáveis a partir da etapa de origem, na ordem configurada. */
  public List<Etapa> destinosValidos(Etapa origem, List<Transicao> transicoes) {
    return transicoes.stream()
        .filter(t -> Objects.equals(t.getEtapaOrigem().getId(), origem.getId()))
        .map(Transicao::getEtapaDestino)
        .toList();
  }
}
