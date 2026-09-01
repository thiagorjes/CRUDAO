package com.crudao.kanban.papel.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssociarUsuarioRequest {
    @NotNull(message = "usuarioId é obrigatório")
    private UUID usuarioId;

    @NotNull(message = "papelId é obrigatório")
    private UUID papelId;
}
