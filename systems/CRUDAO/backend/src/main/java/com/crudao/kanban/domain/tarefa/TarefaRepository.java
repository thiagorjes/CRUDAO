package com.crudao.kanban.domain.tarefa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TarefaRepository extends JpaRepository<Tarefa, UUID> {

    /** "Ativa" = ainda não finalizada (etapa atual não é etapa final) — RN-005. */
    boolean existsByWorkflowIdAndEtapaAtualEtapaFinalFalse(UUID workflowId);

    boolean existsByEtapaAtualIdAndEtapaAtualEtapaFinalFalse(UUID etapaId);

    boolean existsByRaiaIdAndEtapaAtualEtapaFinalFalse(UUID raiaId);

    /**
     * Cards do board de um projeto (RF-001) — projeção via {@code SELECT NEW} em uma única query,
     * sem percorrer associações {@code lazy} (achado do Comitê de Análise — Database, exigência de
     * {@code data-model.md} "Nota de performance"; TASK-04.5). Contagem de queries fixa
     * independentemente do volume de tarefas retornadas.
     */
    @Query(
            "SELECT NEW com.crudao.kanban.domain.tarefa.TarefaBoardItemResponse("
                    + "t.id, t.titulo, t.etapaAtual.id, t.raia.id, t.responsavel.id,"
                    + " t.impedida, t.impedidaDesde, t.iniciada) "
                    + "FROM Tarefa t WHERE t.projeto.id = :projetoId")
    List<TarefaBoardItemResponse> buscarItensDoBoard(@Param("projetoId") UUID projetoId);
}
