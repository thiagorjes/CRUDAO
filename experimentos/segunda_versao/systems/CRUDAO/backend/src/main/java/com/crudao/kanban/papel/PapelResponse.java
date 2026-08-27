package com.crudao.kanban.papel;

import java.util.List;
import java.util.UUID;

/** Papel com seus toggles de permissão (contrato `GET/POST /api/projetos/{projetoId}/papeis`). */
public record PapelResponse(
        UUID id, String chave, String nome, boolean protegido, List<PermissaoToggleResponse> permissoes) {

    public record PermissaoToggleResponse(String chave, boolean habilitada) {}
}
