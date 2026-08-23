package com.crudao.kanban.common;

import java.util.UUID;

/**
 * Porta usada por Projeto/Workflow/Etapa/Raia para aplicar RN-005 (bloquear exclusão se houver
 * tarefas ativas vinculadas).
 *
 * <p>Implementada em {@code domain.tarefa.VerificadorDeTarefasAtivasImpl}, consultando {@code
 * TarefaRepository} (TASK-02.1).
 */
public interface VerificadorDeTarefasAtivas {

  boolean existemTarefasNaEtapa(UUID etapaId);

  boolean existemTarefasNoProjeto(UUID projetoId);

  boolean existemTarefasNaRaia(UUID raiaId);
}
