package com.crudao.kanban.domain.notificacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

    /** Lista de não lidas do usuário (RF-005), mais recentes primeiro. */
    List<Notificacao> findByUsuarioIdAndLidaFalseOrderByCriadoEmDesc(UUID usuarioId);

    Optional<Notificacao> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    /** Usado na exclusão da tarefa (RF-019) — FK (V7) não tem cascade. */
    void deleteByTarefaId(UUID tarefaId);
}
