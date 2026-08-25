package com.crudao.kanban.domain.papel;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Chave composta de {@link UsuarioProjetoPapel}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioProjetoPapelId implements Serializable {

    private UUID usuario;
    private UUID projeto;
    private UUID papel;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UsuarioProjetoPapelId that)) {
            return false;
        }
        return Objects.equals(usuario, that.usuario)
                && Objects.equals(projeto, that.projeto)
                && Objects.equals(papel, that.papel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuario, projeto, papel);
    }
}
