package com.crudao.kanban.domain.tarefa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditoriaTarefaRepository extends JpaRepository<AuditoriaTarefa, UUID> {

  /**
   * Join com {@code Usuario} via {@code usuarioId} solto (mesmo padrão de {@link Observador}) —
   * evita N+1 ao resolver o nome do autor de cada linha do histórico (RF-017).
   */
  @Query(
      "select new com.crudao.kanban.domain.tarefa.AuditoriaTarefaDTO("
          + "a.campo, a.valorAnterior, a.valorNovo, a.usuarioId, u.nome, a.criadoEm) "
          + "from AuditoriaTarefa a left join Usuario u on u.id = a.usuarioId "
          + "where a.tarefa.id = :tarefaId order by a.criadoEm desc")
  List<AuditoriaTarefaDTO> historicoPorTarefa(UUID tarefaId);
}
