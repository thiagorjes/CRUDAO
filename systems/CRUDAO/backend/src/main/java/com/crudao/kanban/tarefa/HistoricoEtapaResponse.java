package com.crudao.kanban.tarefa;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Lead-time por etapa (RF-006, RN-001) — {@code saidaEm} nulo = etapa em andamento. */
public record HistoricoEtapaResponse(
        UUID etapaId, OffsetDateTime entradaEm, OffsetDateTime saidaEm, long leadTimeSegundos) {}
