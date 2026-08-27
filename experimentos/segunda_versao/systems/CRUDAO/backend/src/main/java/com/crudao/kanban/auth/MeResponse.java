package com.crudao.kanban.auth;

import java.util.List;
import java.util.UUID;

/**
 * Contrato de {@code GET /api/me} (RF-014).
 *
 * <p>{@code adminGlobal} exposto para a UI decidir se mostra a ação "criar projeto" (RF-008, exige
 * {@link com.crudao.kanban.domain.usuario.Usuario#isAdminGlobal()}, ADR-007) — decorativo, o
 * backend sempre revalida em {@code POST /api/projetos} (RNF-003).
 */
public record MeResponse(
        UUID id, String nome, String email, boolean adminGlobal, List<ProjetoPapeisResponse> projetos) {

    public record ProjetoPapeisResponse(UUID projetoId, List<String> papeis) {}
}
