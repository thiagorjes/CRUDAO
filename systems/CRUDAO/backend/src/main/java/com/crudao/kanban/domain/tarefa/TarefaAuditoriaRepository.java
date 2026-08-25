package com.crudao.kanban.domain.tarefa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaAuditoriaRepository extends JpaRepository<TarefaAuditoria, UUID> {

    /** Histórico completo da tarefa (RF-017), mais antigo primeiro. */
    List<TarefaAuditoria> findByTarefaIdOrderByDataHora(UUID tarefaId);

    /** Limpa a trilha de auditoria ao excluir a tarefa (RF-019) — FK não tem cascade. */
    void deleteByTarefaId(UUID tarefaId);
}
