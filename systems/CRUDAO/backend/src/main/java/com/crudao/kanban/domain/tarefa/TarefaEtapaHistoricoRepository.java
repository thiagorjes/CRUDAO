package com.crudao.kanban.domain.tarefa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaEtapaHistoricoRepository extends JpaRepository<TarefaEtapaHistorico, UUID> {

    /** Registro aberto (etapa em andamento) da tarefa — RF-006, RN-001. */
    Optional<TarefaEtapaHistorico> findByTarefaIdAndSaidaEmIsNull(UUID tarefaId);

    /**
     * Histórico de etapas da tarefa, com {@code etapa} pré-carregada em uma única query —
     * {@code TarefaService.detalhe} lê {@code h.getEtapa().getId()} por registro; sem o fetch
     * join, cada leitura dispararia uma query lazy adicional, escalando com o volume de histórico
     * (achado do Comitê de Análise — Database, TASK-04.5).
     */
    @EntityGraph(attributePaths = "etapa")
    List<TarefaEtapaHistorico> findByTarefaIdOrderByEntradaEm(UUID tarefaId);

    /** Usado na exclusão da tarefa (RF-019) — FK não tem cascade. */
    void deleteByTarefaId(UUID tarefaId);

    /**
     * Base para o dashboard de lead-time por etapa (RF-007, TASK-06.1) — só ciclos fechados
     * ({@code saidaEm} preenchido) entram na média; etapa em andamento não tem lead-time definitivo
     * ainda. {@code etapa} pré-carregada, mesmo motivo de {@link #findByTarefaIdOrderByEntradaEm}.
     */
    @EntityGraph(attributePaths = "etapa")
    List<TarefaEtapaHistorico> findByTarefaProjetoIdAndSaidaEmIsNotNull(UUID projetoId);
}
