package com.crudao.kanban.domain.tarefa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaRepository extends JpaRepository<Tarefa, UUID> {

    /** "Ativa" = ainda não finalizada (etapa atual não é etapa final) — RN-005. */
    boolean existsByWorkflowIdAndEtapaAtualEtapaFinalFalse(UUID workflowId);

    boolean existsByEtapaAtualIdAndEtapaAtualEtapaFinalFalse(UUID etapaId);

    boolean existsByRaiaIdAndEtapaAtualEtapaFinalFalse(UUID raiaId);
}
