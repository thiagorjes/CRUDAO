package com.crudao.kanban.projeto.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** TL-10 — usuário associado ao projeto, com o papel atual. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioProjetoResponse {
    private UUID usuarioId;
    private String usuarioNome;
    private UUID papelId;
    private String papelNome;
}
