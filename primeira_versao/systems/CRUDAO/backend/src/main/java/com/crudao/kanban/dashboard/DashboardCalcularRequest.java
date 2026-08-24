package com.crudao.kanban.dashboard;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** Período (data início/fim) sobre o qual o dashboard agregado deve ser calculado (RF-007). */
public record DashboardCalcularRequest(@NotNull Instant dataInicio, @NotNull Instant dataFim) {}
