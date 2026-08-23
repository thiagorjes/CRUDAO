package com.crudao.kanban.domain.rbac;

import java.util.UUID;

/**
 * Projeção mínima de {@link Usuario} para resolução de nome/avatar no frontend (RF-001). Sem
 * {@code email} deliberadamente — minimização de dados (achado de code review da TASK-05.1): o
 * board só precisa de nome/inicial, e expor e-mail de todos os usuários a qualquer autenticado
 * facilitaria enumeração.
 */
public record UsuarioDTO(UUID id, String nome) {}
