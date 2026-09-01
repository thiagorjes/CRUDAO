package com.crudao.kanban.papel.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nunca expõe {@code keycloakSub}/{@code adminGlobal}/{@code ativo} (achado do Comitê de Análise,
 * Security — ver docs/techspec/kanban-tarefas/contracts/papeis-permissoes.md).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioBuscaResponse {
    private UUID id;
    private String nome;
    private String email;
}
