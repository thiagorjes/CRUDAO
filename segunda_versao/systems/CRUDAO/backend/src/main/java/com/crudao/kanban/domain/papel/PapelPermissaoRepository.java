package com.crudao.kanban.domain.papel;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PapelPermissaoRepository extends JpaRepository<PapelPermissao, PapelPermissaoId> {

    List<PapelPermissao> findByPapelId(UUID papelId);

    /**
     * Carrega em uma única query os toggles de todos os papéis informados — evita N+1 na resolução
     * de permissões efetivas ({@code PermissaoService}), chamada em todo endpoint de escrita.
     */
    @Query(
            "SELECT pp FROM PapelPermissao pp "
                    + "JOIN FETCH pp.permissao "
                    + "WHERE pp.papel.id IN :papelIds")
    List<PapelPermissao> findByPapelIdIn(@Param("papelIds") Collection<UUID> papelIds);
}
