package com.crudao.kanban.domain.rbac;

import java.util.List;
import java.util.UUID;

/**
 * Perfil do usuário autenticado (RF-015) — papéis/permissões escopados por projeto, resolvidos em
 * uma única query ({@link UsuarioProjetoPapelRepository#findComPapelEPermissoesByUsuarioId}) para
 * evitar N+1.
 */
public record UsuarioMeDTO(UUID id, String nome, boolean admin, List<ProjetoPapeisDTO> projetos) {}
