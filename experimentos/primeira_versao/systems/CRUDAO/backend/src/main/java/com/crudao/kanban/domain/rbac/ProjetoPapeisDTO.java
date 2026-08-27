package com.crudao.kanban.domain.rbac;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Papéis e permissões efetivas do usuário autenticado num projeto — parte de {@link UsuarioMeDTO}.
 */
public record ProjetoPapeisDTO(UUID projetoId, List<String> papeis, Set<String> permissoes) {}
