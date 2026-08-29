package com.crudao.kanban.domain.notificacao;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

    /**
     * Retorna notificações não lidas do usuário, ordenadas por data mais recente.
     */
    List<Notificacao> findByUsuarioIdAndLidaFalseOrderByCriadoEmDesc(UUID usuarioId);

    /**
     * Retorna todas as notificações do usuário, ordenadas por data.
     */
    List<Notificacao> findByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId);

    /**
     * Retorna notificações de uma tarefa específica.
     */
    List<Notificacao> findByTarefaIdOrderByCriadoEmDesc(UUID tarefaId);
}
