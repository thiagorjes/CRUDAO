package com.crudao.kanban.papel;

import java.util.UUID;

/**
 * Resultado de {@code GET /api/projetos/{projetoId}/usuarios/buscar} (RF-015, TASK-07.5) — só os
 * campos necessários para escolher a quem associar; nunca expõe {@code keycloakSub}/{@code
 * adminGlobal}/{@code ativo}.
 */
public record UsuarioResumoResponse(UUID id, String nome, String email) {}
