package com.crudao.kanban.domain.papel;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Chave composta de {@link PapelPermissao}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PapelPermissaoId implements Serializable {

    private UUID papel;
    private UUID permissao;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PapelPermissaoId that)) {
            return false;
        }
        return Objects.equals(papel, that.papel) && Objects.equals(permissao, that.permissao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(papel, permissao);
    }
}
