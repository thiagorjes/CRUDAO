package com.crudao.kanban.domain.papel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioProjetoPapelRepository
        extends JpaRepository<UsuarioProjetoPapel, UsuarioProjetoPapelId> {

    List<UsuarioProjetoPapel> findByUsuarioIdAndProjetoId(UUID usuarioId, UUID projetoId);

    /**
     * Carrega {@code projeto} e {@code papel} via {@code JOIN FETCH} — evita N+1 ao montar {@code
     * GET /api/me} (associações são {@code LAZY} por padrão).
     */
    @Query(
            "SELECT upp FROM UsuarioProjetoPapel upp "
                    + "JOIN FETCH upp.projeto "
                    + "JOIN FETCH upp.papel "
                    + "WHERE upp.usuario.id = :usuarioId")
    List<UsuarioProjetoPapel> findByUsuarioId(@Param("usuarioId") UUID usuarioId);

    /**
     * Vínculos do projeto com {@code usuario} e {@code papel} carregados via {@code JOIN FETCH}
     * (TL-10 — lista de usuários do projeto).
     */
    @Query(
            "SELECT upp FROM UsuarioProjetoPapel upp "
                    + "JOIN FETCH upp.usuario "
                    + "JOIN FETCH upp.papel "
                    + "WHERE upp.projeto.id = :projetoId")
    List<UsuarioProjetoPapel> findByProjetoId(@Param("projetoId") UUID projetoId);

    boolean existsByPapelId(UUID papelId);

    Optional<UsuarioProjetoPapel> findByUsuarioIdAndProjetoIdAndPapelId(
            UUID usuarioId, UUID projetoId, UUID papelId);
}
