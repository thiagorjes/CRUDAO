package com.crudao.kanban.domain.tarefa;

import java.time.Instant;
import java.util.UUID;

/** Linha do histórico de auditoria de uma tarefa — RF-017. */
public record AuditoriaTarefaDTO(
    CampoAuditoria campo,
    String valorAnterior,
    String valorNovo,
    UUID usuarioId,
    String usuarioNome,
    Instant criadoEm) {}
