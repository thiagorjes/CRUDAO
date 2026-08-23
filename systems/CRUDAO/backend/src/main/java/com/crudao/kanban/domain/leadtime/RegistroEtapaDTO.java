package com.crudao.kanban.domain.leadtime;

import java.time.Instant;
import java.util.UUID;

/**
 * Tempo por etapa de uma tarefa, com o tempo em impedimento observado naquela permanência (RF-006).
 */
public record RegistroEtapaDTO(
    UUID id,
    UUID etapaId,
    String etapaNome,
    Instant entradaEm,
    Instant saidaEm,
    long tempoImpedimentoSegundos) {

  static RegistroEtapaDTO de(RegistroEtapa registro) {
    return new RegistroEtapaDTO(
        registro.getId(),
        registro.getEtapa().getId(),
        registro.getEtapa().getNome(),
        registro.getEntradaEm(),
        registro.getSaidaEm(),
        registro.getTempoImpedimentoSegundos());
  }
}
