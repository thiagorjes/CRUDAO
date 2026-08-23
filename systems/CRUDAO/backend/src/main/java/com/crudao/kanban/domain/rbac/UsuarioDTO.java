package com.crudao.kanban.domain.rbac;

import java.util.UUID;

/** Projeção mínima de {@link Usuario} para resolução de nome/avatar no frontend (RF-001). */
public record UsuarioDTO(UUID id, String nome, String email) {}
