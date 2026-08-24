package com.crudao.kanban.domain.rbac;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.NoArgsConstructor;

/**
 * Chave composta de {@link UsuarioProjetoPapel}, na ordem {@code (usuario_id, projeto_id,
 * papel_id)} — ordem relevante para o índice de graça na checagem pontual de {@code
 * AutorizacaoProjetoService} (achado do comitê — database, data-model.md).
 */
@NoArgsConstructor
public class UsuarioProjetoPapelId implements Serializable {

  private UUID usuarioId;
  private UUID projetoId;
  private UUID papelId;

  public UsuarioProjetoPapelId(UUID usuarioId, UUID projetoId, UUID papelId) {
    this.usuarioId = usuarioId;
    this.projetoId = projetoId;
    this.papelId = papelId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UsuarioProjetoPapelId that)) {
      return false;
    }
    return Objects.equals(usuarioId, that.usuarioId)
        && Objects.equals(projetoId, that.projetoId)
        && Objects.equals(papelId, that.papelId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(usuarioId, projetoId, papelId);
  }
}
