package com.crudao.kanban.domain.usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByKeycloakSub(String keycloakSub);

    /**
     * Busca de usuários para associação a um projeto (RF-015, TASK-07.5) — só ativos, exclui quem
     * já tem vínculo com o projeto informado, top 20 por nome. Escopada por {@code projetoId} para
     * nunca virar uma listagem global da base de usuários (achado de segurança da decisão de
     * arquitetura desta task).
     */
    @Query(
            "SELECT u FROM Usuario u WHERE u.ativo = true "
                    + "AND (LOWER(u.nome) LIKE LOWER(CONCAT('%', :q, '%')) "
                    + "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))) "
                    + "AND NOT EXISTS ("
                    + "  SELECT 1 FROM UsuarioProjetoPapel upp "
                    + "  WHERE upp.usuario = u AND upp.projeto.id = :projetoId"
                    + ") "
                    + "ORDER BY u.nome")
    List<Usuario> buscarNaoAssociados(
            @Param("projetoId") UUID projetoId, @Param("q") String q, Pageable limite);
}
