package com.crudao.kanban.domain.rbac;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioProjetoPapelRepository
    extends JpaRepository<UsuarioProjetoPapel, UsuarioProjetoPapelId> {

  List<UsuarioProjetoPapel> findByUsuarioIdAndProjetoId(UUID usuarioId, UUID projetoId);

  /** Usa {@code idx_upp_projeto} — não é prefixo da PK (usuario_id, projeto_id, papel_id). */
  List<UsuarioProjetoPapel> findByProjetoId(UUID projetoId);

  void deleteByUsuarioIdAndProjetoId(UUID usuarioId, UUID projetoId);

  /** Join fetch único para evitar N+1 em {@code GET /api/usuarios/me}. */
  @Query(
      "select v from UsuarioProjetoPapel v join fetch v.papel p join fetch p.permissoes"
          + " where v.usuarioId = :usuarioId")
  List<UsuarioProjetoPapel> findComPapelEPermissoesByUsuarioId(UUID usuarioId);
}
