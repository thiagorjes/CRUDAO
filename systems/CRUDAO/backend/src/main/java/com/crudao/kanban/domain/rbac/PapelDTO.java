package com.crudao.kanban.domain.rbac;

import java.util.Set;
import java.util.UUID;

public record PapelDTO(UUID id, String nome, boolean protegido, Set<String> permissoes) {}
