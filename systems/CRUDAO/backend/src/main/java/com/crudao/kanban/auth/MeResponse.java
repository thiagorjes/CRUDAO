package com.crudao.kanban.auth;

import java.util.List;
import java.util.UUID;

/** Contrato de {@code GET /api/me} (RF-014). */
public record MeResponse(UUID id, String nome, String email, List<ProjetoPapeisResponse> projetos) {

    public record ProjetoPapeisResponse(UUID projetoId, List<String> papeis) {}
}
