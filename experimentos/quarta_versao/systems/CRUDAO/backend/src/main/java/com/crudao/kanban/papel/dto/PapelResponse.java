package com.crudao.kanban.papel.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PapelResponse {
    private UUID id;
    private String chave;
    private String nome;
    private boolean protegido;
    private List<PermissaoToggle> permissoes;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissaoToggle {
        private String chave;
        private boolean habilitada;
    }
}
