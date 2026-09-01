package com.crudao.kanban.papel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TogglePermissaoRequest {
    @NotNull(message = "habilitada é obrigatória")
    private Boolean habilitada;
}
