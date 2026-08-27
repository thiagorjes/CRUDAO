package com.crudao.kanban.domain.rbac;

import java.util.List;
import java.util.UUID;

/** Membro de um projeto (RF-015) — usuário e os papéis que acumula naquele projeto. */
public record MembroDTO(UUID usuarioId, String nome, List<String> papeis) {}
