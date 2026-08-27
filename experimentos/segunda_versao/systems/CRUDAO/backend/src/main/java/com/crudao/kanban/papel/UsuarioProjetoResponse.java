package com.crudao.kanban.papel;

import java.util.List;
import java.util.UUID;

/** Usuário associado a um projeto com os papéis que possui nele (TL-10, RF-015). */
public record UsuarioProjetoResponse(
        UUID usuarioId, String nome, String email, List<String> papeis) {}
