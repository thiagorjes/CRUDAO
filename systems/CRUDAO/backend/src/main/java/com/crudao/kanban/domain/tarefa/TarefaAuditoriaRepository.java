package com.crudao.kanban.domain.tarefa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaAuditoriaRepository extends JpaRepository<TarefaAuditoria, UUID> {}
